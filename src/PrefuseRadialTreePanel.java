
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Hashtable;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import prefuse.Visualization;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.util.FontLib;
import prefuse.util.ui.JFastLabel;

// wrapper for a prefuse radial visualization

public class PrefuseRadialTreePanel<E> extends JPanel implements
      TreeCommon.TreeStructure<E> {

   private static final long serialVersionUID = 2634922661667302752L;

   private Hashtable<E, Node> nodeLookup;
   private TreeCommon.SimpleTree<E> dataTree;

   private final String label = "url";
   private final E root;

   private Graph graph;
   private PrefuseRadialGraphView radialView;
   
   private JFastLabel instructions = new JFastLabel("Left Click + Drag = Drag the graph. Scroll = Zoom. ");
   
   public PrefuseRadialTreePanel(int w, int h, E root, Visualization vis) {

      setLayout(new BorderLayout());

      nodeLookup = new Hashtable<E, Node>();

      graph = new Tree();
      graph.getNodeTable().addColumn(label, String.class);

      this.root = root;
      setRootNode(this.root);

      radialView = new PrefuseRadialGraphView(graph, label, w, h, vis);

      add(radialView, BorderLayout.CENTER);

      Box infoBox = new Box(BoxLayout.X_AXIS);
      
      instructions.setPreferredSize(new Dimension(75,30));
      instructions.setVerticalAlignment(SwingConstants.TOP);
      instructions.setHorizontalAlignment(SwingConstants.CENTER);
      instructions.setFont(FontLib.getFont("Consolas", 14));
      
      infoBox.add(instructions);
      
      add(infoBox, BorderLayout.PAGE_END);
      
   }

   private void doRedraw() {
      radialView.refresh();
   }

   private void setRootNode(E node) {

      if (dataTree == null) {

         dataTree = new TreeCommon.SimpleTree<E>(node);

         Node activeNode = graph.addNode();
         activeNode.set(label, node.toString());
         nodeLookup.put(node, activeNode);

      }

   }

   @Override
   public void addNode(String parentID, E node) {

      addNode((E) getNode(parentID), node);
   }

   @Override
   public void addNode(E parent, E node) {


      if (parent == null) {
         parent = this.root;
      }

      dataTree.addNode(parent, node);

      Node activeNode = graph.addNode();
      activeNode.set(label, node.toString());

      Edge activeEdge = graph.addEdge(nodeLookup.get(parent), activeNode);
      nodeLookup.put(node, activeNode);

      doRedraw();
   }

   @Override
   public List<E> getChildren(E node) {
      return dataTree.getChildren(node);
   }

   @Override
   public E getNode(String id) {
      return (E) dataTree.getNode(id);
   }

   @Override
   public E getRootNode() {
      return (E) dataTree.getRootNode();
   }

   @Override
   public int getTreeSize() {
      if (dataTree == null)
         return 0;
      return dataTree.getTreeSize();
   }

   public E getRandomNode() {
      return (E) dataTree.getRandomNode();
   }


}
