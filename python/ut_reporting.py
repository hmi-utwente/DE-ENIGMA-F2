import rosbag
import json
import re


def ut_report_activities(ts):
    #regular expression patterns for the various message formats that we are interested in
    reStartActivity = re.compile(r"Adult pressed button: \[start_([a-zA-Z]+)\]")
    reCorrect = re.compile(r"Child gave correct answer\. .*")
    reIncorrect = re.compile(r"Child gave incorrect answer\. .*")
    reEndActivity = re.compile(r"Adult pressed button: \[end_([a-zA-Z]+)\]")

    #the topic where our logger writes each log message
    topic = ['ut_logger']
    
    #the dict file that we fill, this will go into the report
    utDict = {
            'ts':ts,
            'ExploreFeatures':{'count':0,'total_duration':0,'correct':0,'incorrect':0},
            'PromptFeatures':{'count':0,'total_duration':0,'correct':0,'incorrect':0},
            'ExploreExpressions':{'count':0,'total_duration':0,'correct':0,'incorrect':0},
            'PromptExpressions':{'count':0,'total_duration':0,'correct':0,'incorrect':0}
        }
    
    #open the bag file where the stream of ROS messages is stored
    #TODO: in case the filename does not match exactly the timestamp, search for the closest "xxx.bag" file
    bag = rosbag.Bag(str(ts)+'.bag')
    
    #start parsing each message, searching for the specific log messages that we need to report
    for topic, msg, t in bag.read_messages(topics=[topic]):
        #message should be JSON string, so try to parse
        try:
            jmsg = json.loads(msg.message)
        except ValueError:
            print("Oops, the message is not in JSON string format... "+str(msg))
            
        activity = 'none'
        
        #search for starting an activity, when found we increase the counter and save the start timestamp
        searchStartActivity = reStartActivity.search(jmsg['message'])
        if searchStartActivity:
            if activity != 'none':
                print("That's strange, a new activity has started before the previous ended...")
            activity = searchStartActivity.group(1)
            actStartTime = jmsg['timestamp']
            utDict[activity]['count'] = utDict[activity].get('count', 0) + 1
        
        searchCorrect = reCorrect.search(jmsg['message'])
        if searchCorrect and activity != 'none':
            utDict[activity]['correct'] = utDict[activity].get('correct', 0) + 1
            
        searchIncorrect = reIncorrect.search(jmsg['message'])
        if searchIncorrect and activity != 'none':
            utDict[activity]['incorrect'] = utDict[activity].get('incorrect', 0) + 1
        
        #search for ending an activity, we then calculate the duration that was spent in this activity
        searchEndActivity = reEndActivity.search(jmsg['message'])
        if searchEndActivity and activity == searchEndActivity.group(1):
            duration = jmsg['timestamp'] - actStartTime
            utDict[activity]['total_duration'] = utDict[activity].get('total_duration', 0) + duration
            activity = 'none'
            
    return utDict