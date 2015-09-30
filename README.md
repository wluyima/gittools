gittools
========

Contains utilities for interacting with a git repo programmatically

USAGE:
======

1- Add all modules to module_previous_versions.properties file with the module id as the key and its version in the
previous release of the reference application as the value, note that the value is actually the tag's name
in github which can be different from just the version number e.g appointmentscheduling-1.3 instead of 1.3

2- Run GitHistoryLoader, this will pull all the necessary commits and create local copies of them in the
cache directory at the root

3- Copy the contents in from cache directory to preloaded

4- Run ReferenceApplicationContributors
