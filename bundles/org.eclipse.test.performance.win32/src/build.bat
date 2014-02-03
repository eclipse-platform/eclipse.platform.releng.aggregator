@rem ***************************************************************************
@rem Copyright (c) 2014 IBM Corporation and others.
@rem All rights reserved. This program and the accompanying materials
@rem are made available under the terms of the Eclipse Public License v1.0
@rem which accompanies this distribution, and is available at
@rem http://www.eclipse.org/legal/epl-v10.html
@rem
@rem Contributors:
@rem     IBM Corporation - initial API and implementation
@rem ***************************************************************************
@echo off
if '%1' == '?' goto help
if not '%BATLST%' == '' echo on
rem - build        - Build the program

del ivjperf.h
call javah.exe -classpath "z:\jars\perfmsr.jar" -jni -o ivjperf.h org.eclipse.perfmsr.core.PerformanceMonitor
echo After running this step you still need to do a build inside of VC
@goto exit

:help
echo Build the program
goto exit

:exit