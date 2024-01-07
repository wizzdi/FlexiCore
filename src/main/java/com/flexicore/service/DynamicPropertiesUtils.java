package com.flexicore.service;


import java.util.HashMap;
import java.util.Map;

public class DynamicPropertiesUtils {


    /**
     * returns a set of merged values from two maps if there was an update otherwise null
     * @param newVals incoming values
     * @param current existing values
     * @return merged map or null
     */
    public static Map<String, Object> updateDynamic(Map<String, Object> newVals, Map<String, Object> current) {
        boolean update = false;
        if (newVals != null && !newVals.isEmpty()) {
            if (current == null) {
                return newVals;
            } else {
                Map<String, Object> copy = new HashMap<>(current);
                for (Map.Entry<String, Object> entry : newVals.entrySet()) {
                    String key = entry.getKey();
                    Object newVal = entry.getValue();
                    Object val = current.get(key);
                    if ((newVal==null&&val!=null)|| (newVal!=null&&!newVal.equals(val))) {
                        copy.put(key, newVal);
                        update = true;
                    }
                }
                if (update) {
                    return copy;
                }
            }


        }
        return null;
    }
}
