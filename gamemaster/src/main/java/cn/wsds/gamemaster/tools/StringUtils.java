package cn.wsds.gamemaster.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lidahe on 15/12/26.
 */
public class StringUtils {
    private static final String SEPARATOR = ",";

    public static List<String> split(String origin, String separator) {
        if(origin == null) {
            return null;
        }
        if(origin.contains(separator)) {
            return new ArrayList<String>(Arrays.asList(origin.split(separator)));
        }
        return null;
    }

    public static List<String> split(String origin) {
        return split(origin, SEPARATOR);
    }

    public static String make(List<String> array) {
    	if (array == null || array.isEmpty()) {
    		return null;
    	}
        StringBuffer sb = new StringBuffer(512);
        for(String str : array) {
            sb.append(str);
            sb.append(SEPARATOR);
        }
        return sb.toString();
    }
    
   public static  StringBuilder getStringBuilder(String label){
		
		StringBuilder buidler= new StringBuilder();
		if((label!=null)&&(!label.isEmpty())){
			buidler.append(label);
		}
		
		return buidler;
	}

//    public static List<String> remove(List<String> origin, String... dels) {
//        List<String> result = origin;
//        for(String del : dels) {
//            result = remove(result, del);
//        }
//        return result;
//    }
//
//    public static List<String> remove(List<String> origin, String del) {
//        List<String> list = new ArrayList<String>();
//        for(String str : origin) {
//            if (!str.equals(del)){
//                list.add(str);
//            }
//        }
//        return list;
//    }

}
