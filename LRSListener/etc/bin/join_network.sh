#!/bin/bash

# this script is autogenerated by 'ant startscripts'
# it starts a LAS2peer node providing the service 'i5.las2peer.services.gamificationAchievementService.GamificationAchievementService' of this project
# pls execute it from the root folder of your deployment, e. g. ./bin/start_network.sh

java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher --port 9013 -b 10.0.2.15:9011 --service-directory service uploadStartupDirectory startService\(\'i5.las2peer.services.gamification.listener.LRSListener@0.1\'\)
