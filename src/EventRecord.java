import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventRecord {

    public Long pcId;
    public Integer processId;
    public Integer threadId;

//    public String processName;

    public Long timeStamp;

    public String eventName;

    public List<CharSequence> callstack;

    public Map<String,String> arguments;
    public EventRecord(){
            arguments = new HashMap<>();
            eventName="";
            callstack=new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder one= new StringBuilder("{" +
                "\"pcId\":" + pcId +
                ", \"processId\":" + processId +
                ", \"threadId\":" + threadId +
                ", \"timeStamp\":" + timeStamp +
                ", \"eventName\":\"" + eventName + "\"" +
                ", \"callstack\":\"" + callstack.toString().replace("\\", "\\\\") + "\"" +
                ", \"arguments\":{");
        boolean removeLastChar=false;
        for (String key:arguments.keySet()) {
            if(arguments.get(key)!=null) {
                one.append("\"").append(key).append("\":\"").append(arguments.get(key).replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
                removeLastChar = true;
            }
        }
        if(removeLastChar){
            one.deleteCharAt(one.length() - 1);
        }
        one.append("}}");
        return one.toString();
    }
}
