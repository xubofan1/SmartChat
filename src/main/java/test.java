import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.*;


public class test {
    public static void main(String[] args) {
        String s = "cbaebabacd";
        String p = "abc";
        List<Integer> list = findAnagrams(s,p);
        System.out.println(list);
    }

    public static List<Integer> findAnagrams(String s, String p) {

        char[] ch = p.toCharArray();
        Arrays.sort(ch);
        String p1 = new String(ch);
        System.out.println(p1);



        int num = p.length();

        List<Integer> list = new ArrayList<>();



        for(int i = 0;i<s.length()-num;i++){

            String s1 = s.substring(i,i+num);
            char[] c = s1.toCharArray();
            Arrays.sort(c);
            s1 = new String(c);


            if(s1.equals(p1)) list.add(i);
        }
        return list;

    }
}
