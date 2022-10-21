job('Cleanup/cleanupReleaseArtifacts'){
  displayName('Cleanup Release Artifacts')
  description('Cleanup major artifacts from previous releases at the beginning of a new release.')

  label('migration')

  logRotator {
    daysToKeep(1)
    numToKeep(1)
  }

  parameters {
    stringParam('release_to_clean', null, 'Previous release to be cleaned up.\r\n For example: 4.25')
    stringParam('release_build', null, 'I-build that was promoted to release.\r\n For example: I20220831-1800')
    stringParam('release_to_remove', null, 'The Eclipse Project Downloads page only keeps 3 latest releases. Specify the release to remove (release_to_clean - 3) here.\r\n For example: 4.22')
    booleanParam('remove_y_builds', false, '''Typically only true in even releases.
    True: Remove Y-build directories and move P-build directory to P-builds-old for safekeeping.
    False: Remove P-builds-old directory.
    ''' )
    stringParam('p_build', null, 'Current P-Build version so that it can be removed or archived.\r\n For Example: 4.23 would be removed during the 4.25 release.')
  }

  triggers {
    cron('''
      TZ=America/Toronto
      # format: Minute Hour Day Month Day of the week (0-7)

      #Run at 8am the day of the GA release
      0 8 14 09 3 #2022-09
      0 8 07 12 3 #2022-12
    ''')
  }

  wrappers { //adds pre/post actions
    preBuildCleanup()
    timestamps()
    sshAgent('projects-storage.eclipse.org-bot-ssh')
  }

  steps {
    shell('''
      #!/bin/bash -x

      #update sites
      ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/updates/${release_to_clean}-I-builds/
      ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/updates/index.html

      if [[ $remove_y_builds = true ]]; then
        #notes: only rename ${release_to_clean}-P-builds on even releases (4.18, 4.20, 4.22, etc); skip rename on odd releases (4.19, 4.21, 4.23, etc)
        ssh genie.releng@projects-storage.eclipse.org mv /home/data/httpd/download.eclipse.org/eclipse/updates/${p_build}-P-builds /home/data/httpd/download.eclipse.org/eclipse/updates/${p_build}-P-builds-old

        #notes: only remove ${release_to_clean}-Y-builds on even releases (4.18, 4.20, 4.22, etc); skip remove on odd releases (4.19, 4.21, 4.23, etc)
        ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/updates/${release_to_clean}-Y-builds/
        ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/updates/${release_to_clean}-P-builds/
      else
        #delete old P-builds on odd releases (4.19, 4.21, 4.23, etc)
        ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/updates/${p_build}-P-builds-old/
        ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/updates/${p_build}-Y-builds/
      fi

      cd ${WORKSPACE}
      cat <<EOT > index.html
      <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
      <html>
        <head>
          <title>Eclipse</title>
          <meta http-equiv="Cache-control" content="no-cache, no-store, must-revalidate">
          <meta http-equiv="REFRESH" content="0;url=https://download.eclipse.org/eclipse/updates/${release_to_clean}/" />
        </head>
        <body>
          This page will redirect to <a href="https://download.eclipse.org/eclipse/updates/${release_to_clean}/">current software repository</a>.
        </body>
      </html>
      EOT
      scp index.html genie.releng@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/eclipse/updates/

      #eclipse downloads
      ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/S-${release_to_clean}*
      ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/R-${release_to_remove}*

      #eclipse downloads
      ibuilds=$(ssh genie.releng@projects-storage.eclipse.org ls /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/)
      idate=${release_build:1:8}

      for i in ibuilds
      do
        if [ idate -ge ${i:1:8} ]
        then
          ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/$i
        fi
      done

      #equinox downloads
      ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/equinox/drops/S-${release_to_clean}*
      ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/equinox/drops/R-${release_to_remove}*
      ssh genie.releng@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/equinox/drops/Y*

    ''')
  }
}
