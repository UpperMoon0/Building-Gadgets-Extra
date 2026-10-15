#!/usr/bin/env python3
"""Build the pinned standalone UI and transport mods for this native target."""
import argparse
import os
from pathlib import Path
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
TARGETS=('forge-1.16.5','forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2')

def run_gradle(folder,tasks,target,offline):
    wrapper=folder/('gradlew.bat' if os.name=='nt' else 'gradlew')
    env=os.environ.copy()
    # Gradle 8 runs on Java 21; the independent 26.x transport build runs on Java 25.
    version='25' if target=='neoforge-26.1.2' else '21'
    home=env.get('JAVA_HOME_'+version+'_X64') or env.get('JAVA'+version+'_HOME')
    if home: env['JAVA_HOME']=home
    command=([str(wrapper)] if os.name=='nt' else ['sh',str(wrapper)])+tasks+['--no-daemon']
    if offline: command.append('--offline')
    subprocess.run(command,cwd=folder,env=env,check=True)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target',required=True,choices=TARGETS)
    parser.add_argument('--offline',action='store_true')
    args=parser.parse_args()
    for repo in ('PacketWeave','OpenUI-MC'):
        if not (ROOT/'vendor'/repo/'gradlew').is_file():
            raise SystemExit('Initialize pinned dependencies first: git submodule update --init --recursive')
    run_gradle(ROOT/'vendor/PacketWeave/targets'/args.target,['build'],args.target,args.offline)
    ui=ROOT/'vendor/OpenUI-MC'
    if args.target=='forge-1.16.5':
        run_gradle(ui/args.target,['build','publishToMavenLocal'],args.target,args.offline)
    else:
        run_gradle(ui,[':'+args.target+':build',':'+args.target+':publishToMavenLocal'],args.target,args.offline)

if __name__=='__main__': main()
