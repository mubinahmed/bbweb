/** Specimen Group helpers */
define([], function() {
  'use strict';

  UserViewerFactory.$inject = ['EntityViewer'];

  /**
   * Displays a study annotation type in a modal.
   *
   */
  function UserViewerFactory(EntityViewer) {

    function UserViewer(user) {
      this.ev = new EntityViewer(user, 'User');
      this.ev.addAttribute('Name', user.name);
      this.ev.addAttribute('Email',  user.email);
      this.ev.addAttribute('Status', user.status);

      this.ev.showModal();
    }

    return UserViewer;
  }

  return UserViewerFactory;
});
