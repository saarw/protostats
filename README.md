# protostats
Project to crowd source and log mobile phone network stats. Project consisted of  
- Android app that reports phone network communication timings and network provider info to the backend
- Go backend that provides a JSON-RPC API to collect stats from the Android app and writes the stats to a log
- Angular web UI that shows the statistics collected by the backend
