#!/usr/bin/env python3

# A wrapper for the gradle wrapper...this script allows you to just execute
# `python3 run.py` on Windows and *nix.
#
# Erik H

import os
import subprocess
import sys

# Get all of the command line arguments quoted and concatenated together
args = ",".join("'{}'".format(a) for a in map(str, sys.argv[1:]))
# Use the appropriate gradle wrapper based on what OS we're running
gradle_script = "gradlew.bat" if os.name == "nt" else "./gradlew"
run_command = [gradle_script, "-q", "run", "--console=plain"]

if not args:
    subprocess.call(run_command)
else:
    # Pass the args on to the gradle wrapper
    subprocess.call(run_command + ["-PappArgs=[{}]".format(args)])
