package org.biobank.domain

package study {

  trait HasStudyId {

    /** The ID of the study this object belongs to. */
    val studyId: StudyId

  }

  trait HasSpecimenGroupId {

    /** The ID of the study this object belongs to. */
    val specimenGroupId: SpecimenGroupId

  }

  trait HasParticipantId {

    /** The ID of the study this object belongs to. */
    val participantId: ParticipantId

  }

}
