
// too noisy? just set this to false...

public class Debug {

   public static Boolean doPrint = true;

   public static void out(String s) {
      if (doPrint)
         System.out.println(s);
   }

   public static void err(String s) {
      if (doPrint)
         System.err.println(s);
   }
}
