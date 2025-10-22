// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class EventFileHandler {
   private final String filePath;

   public EventFileHandler() {
      this("events.csv");
   }

   public EventFileHandler(String var1) {
      this.filePath = var1;
      this.ensureFileExistsWithDefaults();
   }

   private void ensureFileExistsWithDefaults() {
      File var1 = new File(this.filePath);
      if (!var1.exists()) {
         List var2 = Arrays.asList("Coding", "Dance", "Debate", "Robotics");
         this.saveEvents(var2);
      }

   }

   public List<String> loadEvents() {
      ArrayList var1 = new ArrayList();
      File var2 = new File(this.filePath);
      if (!var2.exists()) {
         return var1;
      } else {
         try {
            BufferedReader var3 = new BufferedReader(new FileReader(var2));

            String var4;
            try {
               while((var4 = var3.readLine()) != null) {
                  var4 = var4.trim();
                  if (!var4.isEmpty()) {
                     var1.add(var4);
                  }
               }
            } catch (Throwable var7) {
               try {
                  var3.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }

               throw var7;
            }

            var3.close();
         } catch (IOException var8) {
            var8.printStackTrace();
         }

         return var1;
      }
   }

   public boolean saveEvents(List<String> var1) {
      try {
         PrintWriter var2 = new PrintWriter(new FileWriter(this.filePath, false));

         boolean var8;
         try {
            Iterator var3 = var1.iterator();

            while(true) {
               if (!var3.hasNext()) {
                  var8 = true;
                  break;
               }

               String var4 = (String)var3.next();
               var2.println(var4);
            }
         } catch (Throwable var6) {
            try {
               var2.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         var2.close();
         return var8;
      } catch (IOException var7) {
         var7.printStackTrace();
         return false;
      }
   }

   public boolean addEvent(String var1) {
      if (var1 == null) {
         return false;
      } else {
         var1 = var1.trim();
         if (var1.isEmpty()) {
            return false;
         } else {
            List var2 = this.loadEvents();
            if (var2.contains(var1)) {
               return false;
            } else {
               var2.add(var1);
               return this.saveEvents(var2);
            }
         }
      }
   }

   public boolean renameEvent(String var1, String var2) {
      if (var1 != null && var2 != null) {
         var2 = var2.trim();
         if (var2.isEmpty()) {
            return false;
         } else {
            List var3 = this.loadEvents();
            if (!var3.contains(var1)) {
               return false;
            } else if (var3.contains(var2)) {
               return false;
            } else {
               int var4 = var3.indexOf(var1);
               var3.set(var4, var2);
               return this.saveEvents(var3);
            }
         }
      } else {
         return false;
      }
   }

   public boolean removeEvent(String var1) {
      List var2 = this.loadEvents();
      boolean var3 = var2.remove(var1);
      return var3 ? this.saveEvents(var2) : false;
   }
}
