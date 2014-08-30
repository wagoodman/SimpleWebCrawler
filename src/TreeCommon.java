
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Queue;
import java.util.Random;

// a simple tree structure and crawling strategy

public class TreeCommon {

   @SuppressWarnings("hiding")
   public static interface TreeStructure<E> {
      public void addNode(E parent, E node);

      public void addNode(String parentID, E node);

      public E getRootNode();

      public E getNode(String id);

      public List<E> getChildren(E node);

      public int getTreeSize();

   }

   @SuppressWarnings("hiding")
   public static interface TreeCrawler<TreeStructure, E> {

      public void crawl(Queue<E> queue);

      public List<E> visit(E input);

   }

   @SuppressWarnings("rawtypes")
   public static class SimpleTree<E> implements TreeStructure<E> {

      private E root;
      private Hashtable<E, List<E>> map;

      public SimpleTree(E rootNode) {
         map = new Hashtable<E, List<E>>();
         root = rootNode;
         map.put(root, new ArrayList<E>());
      }

      @Override
      public void addNode(String parentID, E node) {
         addNode((E) getNode(parentID), node);
      }

      @Override
      public void addNode(E parent, E node) {

         if (parent != null && !map.containsKey(parent)) {
            throw new IllegalArgumentException("Parent does not exist. ("
                  + String.valueOf(parent) + ")");
         }

         if (parent != null) {
            // append a new child node to the parent
            map.get(parent).add(node);
         }

         // add node to the map
         map.put(node, new ArrayList<E>());

      }

      @Override
      public List<E> getChildren(E node) {
         if (!map.containsKey(node)) {
            throw new IllegalArgumentException("Parent does not exist. ("
                  + String.valueOf(node) + ")");
         }

         return map.get(node);
      }

      @Override
      public E getNode(String id) {
         for (E node : map.keySet()) {

            if (node.toString().equals(id)) {
               return node;
            }
         }
         return null;
      }

      public E getRootNode() {
         return root;
      }

      public E getRandomNode() {
         return (E) map.keySet().toArray()[new Random().nextInt(map.keySet()
               .size())];
      }

      @Override
      public int getTreeSize() {
         return map.size();
      }

   }

}
