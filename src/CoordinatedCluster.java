
import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Membership;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.stack.MembershipChangePolicy;
import org.jgroups.util.PayloadUUID;

// responsible for coordinating multiple processes accomplishing a task using jgroups

public class CoordinatedCluster {

   public static class ExitCommand implements Serializable {
   }

   public static class ExhaustedCommand implements Serializable {
   }

   public static class UnexhaustedCommand implements Serializable {
   }

   private final JChannel channel;
   private final ReceiverAdapter userRecvAdaptor;
   private Hashtable<Address, Boolean> exhaustedLookup;

   ReceiverAdapter recvr = new ReceiverAdapter() {
      @Override
      public void receive(Message msg) {
         Debug.out("   received msg from " + msg.getSrc() + ": "
               + msg.getObject());

         // user message handler...
         if (userRecvAdaptor != null)
            userRecvAdaptor.receive(msg);

         // native message handler
         // special case...
         if (msg.getObject() instanceof ExitCommand) {
            disconnect();
         } else if (msg.getObject() instanceof ExhaustedCommand) {
            exhaustedLookup.put(msg.getSrc(), true);
         } else if (msg.getObject() instanceof UnexhaustedCommand) {
            exhaustedLookup.put(msg.getSrc(), false);
         }

      }

      @Override
      public void viewAccepted(View view) {
         for (Address addr : view.getMembers()) {
            if (!exhaustedLookup.containsKey(addr)) {
               // System.err.println("Set Exhausted " + addr);
               exhaustedLookup.put(addr, false);
            }

         }
      }

      @Override
      public void suspect(Address addr) {
         System.err.println("ADDRESS HAS CRASHED " + addr);
      }
   };

   AddressGenerator addressGen = new AddressGenerator() {
      public Address generateAddress() {
         return PayloadUUID.randomUUID(channel.getName(), "CrawlerWorker");
      }
   };

   CoordinatedCluster(String clusterName) throws Exception {
      this(clusterName, null);
   }

   CoordinatedCluster(String clusterName, ReceiverAdapter ra) throws Exception {
      userRecvAdaptor = ra;

      exhaustedLookup = new Hashtable<Address, Boolean>();

      // channel = new JChannel("udp.xml");
      // channel = new JChannel("tcp.xml");
      channel = new JChannel();
      channel.setReceiver(recvr);
      // channel.setAddressGenerator(addressGen);
      channel.connect(clusterName);

      Debug.out("Coordinator: " + getCoordinator() + " my address: "
            + getMyAddress() + "  my Hash Index: " + getHashIndex());
   }

   public Address getMyAddress() {
      return channel.getAddress();
   }

   public Address getCoordinator() {
      return channel.getView().getCreator();
   }

   private int lastClusterSize = -1;
   private int myHashIndex;

   // must be able to handle merges on the fly!
   public Integer getHashIndex() {
      if (lastClusterSize != channel.getView().size()) {
         lastClusterSize = channel.getView().size();
         myHashIndex = channel.getView().getMembers().indexOf(getMyAddress());
      }
      return myHashIndex;
   }

   public Integer getHashIndexOfObj(Object obj) {
      return Math.abs(obj.hashCode()) % channel.getView().getMembers().size();
   }

   // send to a specified Address based on the hash of the object
   public void sendToCorrectWorker(Serializable obj) throws Exception {
      send(getHashIndexOfObj(obj), obj);
   }

   // send to everyone
   public void send(Serializable obj) throws Exception {
      send(null, obj);
   }

   // send explicitly to one address
   public void send(Address to, Serializable obj) throws Exception {
      channel.send(new Message(to, obj));
   }

   // send explicitly to one address based on the order of the members
   public void send(int toHashIndex, Serializable msg) throws Exception {
      channel.send(new Message(channel.getView().getMembers().get(toHashIndex),
            msg));
   }

   public int getMemberCount() {
      return channel.getView().getMembers().size();
   }

   public boolean isExhausted() {
      return exhaustedLookup.get(getMyAddress());
   }

   public void setExhausted(boolean exhausted) {
      try {
         exhaustedLookup.put(getMyAddress(), exhausted);

         if (exhausted)
            send(new ExhaustedCommand());
         else
            send(new UnexhaustedCommand());

         if (everyoneIsExhaused(-1)) {
            Debug.err("Exhausted: " + exhaustedLookup);
            endCluster();
         }
      } catch (Exception e) {
         System.err.println("Set Exhausted Failed");
      }
   }

   public boolean everyoneIsExhaused(int workerCount) {
      // not everyone has even started working! can't be exhausted!
      // if (getMemberCount() < workerCount)
      // return false;

      boolean allExhaused = true;
      for (Boolean isEx : exhaustedLookup.values()) {
         allExhaused &= isEx;
      }
      return allExhaused;
   }

   public void endCluster() {
      try {
         send(new ExitCommand());
      } catch (Exception e) {
      }
   }

   public void disconnect() {
      System.err.println("EXITING: " + getMyAddress());
      channel.disconnect();
      channel.close();
   }

}
