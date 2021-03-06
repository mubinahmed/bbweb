package org.biobank.service.study

import org.biobank.infrastructure.command.StudyCommands._
import org.biobank.infrastructure.event.StudyEvents._
import org.biobank.service.{ Processor, WrappedEvent }
import org.biobank.domain._
import org.biobank.domain.user.UserId
import org.biobank.domain.study._
import org.biobank.domain.study.Study
import org.biobank.domain.AnnotationValueType._

import akka.persistence.SnapshotOffer
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import scaldi.akka.AkkaInjectable
import scaldi.{Injectable, Injector}
import scalaz._
import scalaz.Scalaz._
import scalaz.Validation.FlatMap._

/**
  * The SpecimenLinkAnnotationTypeProcessor is responsible for maintaining state changes for all
  * [[org.biobank.domain.study.SpecimenLinkAnnotationType]] aggregates. This particular processor uses
  * Akka-Persistence's [[akka.persistence.PersistentActor]]. It receives Commands and if valid will persist
  * the generated events, afterwhich it will updated the current state of the
  * [[org.biobank.domain.study.SpecimenLinkAnnotationType]] being processed.
  *
  * It is a child actor of [[org.biobank.service.study.StudiesProcessorComponent.StudiesProcessor]].
  */
class SpecimenLinkAnnotationTypeProcessor(implicit inj: Injector)
    extends StudyAnnotationTypeProcessor[SpecimenLinkAnnotationType]
    with AkkaInjectable {
  import org.biobank.infrastructure.event.StudyEventsUtil._
  import StudyEvent.EventType

  override def persistenceId = "specimen-link-annotation-type-processor-id"

  override val annotationTypeRepository = inject [SpecimenLinkAnnotationTypeRepository]

  val specimenLinkTypeRepository = inject [SpecimenLinkTypeRepository]

  case class SnapshotState(annotationTypes: Set[SpecimenLinkAnnotationType])

  /**
    * These are the events that are recovered during journal recovery. They cannot fail and must be
    * processed to recreate the current state of the aggregate.
    */
  val receiveRecover: Receive = {
    case event: StudyEvent => event.eventType match {
      case et: EventType.SpecimenLinkAnnotationTypeAdded =>
        applySpecimenLinkAnnotationTypeAddedEvent(event)
      case et: EventType.SpecimenLinkAnnotationTypeUpdated =>
        applySpecimenLinkAnnotationTypeUpdatedEvent(event)
      case et: EventType.SpecimenLinkAnnotationTypeRemoved =>
        applySpecimenLinkAnnotationTypeRemovedEvent(event)

        case event => log.error(s"event not handled: $event")
      }

    case SnapshotOffer(_, snapshot: SnapshotState) =>
      snapshot.annotationTypes.foreach{ annotType => annotationTypeRepository.put(annotType) }
  }

  /**
    * These are the commands that are requested. A command can fail, and will send the failure as a response
    * back to the user. Each valid command generates one or more events and is journaled.
    */
  val receiveCommand: Receive = {
    case cmd: AddSpecimenLinkAnnotationTypeCmd =>    processAddSpecimenLinkAnnotationTypeCmd(cmd)
    case cmd: UpdateSpecimenLinkAnnotationTypeCmd => processUpdateSpecimenLinkAnnotationTypeCmd(cmd)
    case cmd: RemoveSpecimenLinkAnnotationTypeCmd => processRemoveSpecimenLinkAnnotationTypeCmd(cmd)

    case "snap" =>
      saveSnapshot(SnapshotState(annotationTypeRepository.getValues.toSet))
      stash()

    case cmd => log.error(s"SpecimenLinkAnnotationTypeProcessor: message not handled: $cmd")
  }

  private def processAddSpecimenLinkAnnotationTypeCmd
    (cmd: AddSpecimenLinkAnnotationTypeCmd): Unit = {
    val timeNow = DateTime.now
    val id = annotationTypeRepository.nextIdentity
    val event = for {
      nameValid <- nameAvailable(cmd.name, StudyId(cmd.studyId))
      newItem <- SpecimenLinkAnnotationType.create(
        StudyId(cmd.studyId), id, -1L, timeNow, cmd.name, cmd.description, cmd.valueType,
        cmd.maxValueCount, cmd.options)
      event <- createStudyEvent(newItem.studyId, cmd).withSpecimenLinkAnnotationTypeAdded(
        SpecimenLinkAnnotationTypeAddedEvent(
          annotationTypeId = Some(newItem.id.id),
          name             = Some(newItem.name),
          description      = newItem.description,
          valueType        = Some(newItem.valueType.toString),
          maxValueCount    = newItem.maxValueCount,
          options          = newItem.options)).success
    } yield event

    process(event) { applySpecimenLinkAnnotationTypeAddedEvent(_) }
  }


  private def processUpdateSpecimenLinkAnnotationTypeCmd
    (cmd: UpdateSpecimenLinkAnnotationTypeCmd): Unit = {
    val timeNow = DateTime.now
    val v = update(cmd) { at =>
      for {
        nameAvailable <- nameAvailable(cmd.name, StudyId(cmd.studyId), AnnotationTypeId(cmd.id))
        newItem <- at.update(cmd.name,
                             cmd.description,
                             cmd.valueType,
                             cmd.maxValueCount,
                             cmd.options)
        event <- createStudyEvent(newItem.studyId, cmd).withSpecimenLinkAnnotationTypeUpdated(
          SpecimenLinkAnnotationTypeUpdatedEvent(
            annotationTypeId = Some(newItem.id.id),
            version          = Some(newItem.version),
            name             = Some(newItem.name),
            description      = newItem.description,
            valueType        = Some(newItem.valueType.toString),
            maxValueCount    = newItem.maxValueCount,
            options          = newItem.options)).success
      } yield event
    }

    process(v) { applySpecimenLinkAnnotationTypeUpdatedEvent(_) }
  }

  private def processRemoveSpecimenLinkAnnotationTypeCmd
    (cmd: RemoveSpecimenLinkAnnotationTypeCmd): Unit = {
    val v = update(cmd) { at =>
      createStudyEvent(at.studyId, cmd).withSpecimenLinkAnnotationTypeRemoved(
        SpecimenLinkAnnotationTypeRemovedEvent(Some(at.id.id))).success
    }
    process(v) { applySpecimenLinkAnnotationTypeRemovedEvent(_) }
  }

  /** Updates to annotation types only allowed if they are not being used by any participants.
    */
  def update
    (cmd: StudyAnnotationTypeModifyCommand)
    (fn: SpecimenLinkAnnotationType => DomainValidation[StudyEvent])
      : DomainValidation[StudyEvent] = {
    for {
      annotType    <- annotationTypeRepository.withId(StudyId(cmd.studyId), cmd.id)
      notInUse     <- checkNotInUse(annotType)
      validVersion <- annotType.requireVersion( cmd.expectedVersion)
      event        <- fn(annotType)
    } yield event
  }

  private def applySpecimenLinkAnnotationTypeAddedEvent(event: StudyEvent) : Unit = {
    if (event.eventType.isSpecimenLinkAnnotationTypeAdded) {
      val addedEvent = event.getSpecimenLinkAnnotationTypeAdded

      annotationTypeRepository.put(
        SpecimenLinkAnnotationType(
          studyId       = StudyId(event.id),
          id            = AnnotationTypeId(addedEvent.getAnnotationTypeId),
          version       = 0L,
          timeAdded     = ISODateTimeFormat.dateTime.parseDateTime(event.getTime),
          timeModified  = None,
          name          = addedEvent.getName,
          description   = addedEvent.description,
          valueType     = AnnotationValueType.withName(addedEvent.getValueType),
          maxValueCount = addedEvent.maxValueCount,
          options       = addedEvent.options))
      ()
    } else {
      log.error(s"invalid event type: $event")
    }
  }

  private def applySpecimenLinkAnnotationTypeUpdatedEvent(event: StudyEvent) : Unit = {
    if (event.eventType.isSpecimenLinkAnnotationTypeUpdated) {
      val updatedEvent = event.getSpecimenLinkAnnotationTypeUpdated

      annotationTypeRepository.getByKey(AnnotationTypeId(updatedEvent.getAnnotationTypeId)).fold(
        err => log.error(s"updating annotatiotn type from event failed: $err"),
        at => {
          annotationTypeRepository.put(
            at.copy(version       = updatedEvent.getVersion,
                    name          = updatedEvent.getName,
                    description   = updatedEvent.description,
                    valueType     = AnnotationValueType.withName(updatedEvent.getValueType),
                    maxValueCount = updatedEvent.maxValueCount,
                    options       = updatedEvent.options,
                    timeModified  = Some(ISODateTimeFormat.dateTime.parseDateTime(event.getTime))))
          ()
        }
      )
    } else {
      log.error(s"invalid event type: $event")
    }
  }

  private def applySpecimenLinkAnnotationTypeRemovedEvent(event: StudyEvent) : Unit = {
    applyStudyAnnotationTypeRemovedEvent(
      AnnotationTypeId(event.getSpecimenLinkAnnotationTypeRemoved.getAnnotationTypeId))
  }

  def checkNotInUse(annotationType: SpecimenLinkAnnotationType)
      : DomainValidation[SpecimenLinkAnnotationType] = {
    if (specimenLinkTypeRepository.annotationTypeInUse(annotationType)) {
      DomainError(s"annotation type is in use by specimen link type: ${annotationType.id}").failureNel
    } else {
      annotationType.success
    }
  }

}
