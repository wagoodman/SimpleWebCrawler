
import java.util.Iterator;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.GroupAction;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.PolarLocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;

import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.HoverActionControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

// prefuse radial visualization

public class PrefuseRadialGraphView extends Display {

   private static final int DURATION = 300;

   public static final String tree = "tree";
   public static final String treeNodes = "tree.nodes";
   public static final String treeEdges = "tree.edges";
   public static final String linear = "linear";

   private LabelRenderer m_nodeRenderer;
   private EdgeRenderer m_edgeRenderer;

   private String m_label = "label";
   public Graph treeRef;
   public PrefuseRadialGraphView gview;
   public RadialTreeLayout treeLayout;

   public void refresh() {
      m_vis.run("filter");
      m_vis.cancel("animatePaint");
      m_vis.run("recolor");
      m_vis.run("animatePaint");
      m_vis.run("repaint");
   }

   public PrefuseRadialGraphView(Graph g, String label, int w, int h,
         Visualization vis) {
      super(vis);
      m_label = label;

      // -- set up visualization --
      treeRef = g;
      m_vis.add(tree, treeRef);
      m_vis.setInteractive(treeEdges, null, false);

      // -- set up renderers --
      m_nodeRenderer = new LabelRenderer(m_label);
      m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
      m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
      m_nodeRenderer.setRoundedCorner(8, 8);
      m_edgeRenderer = new EdgeRenderer();

      DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
      rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
      m_vis.setRendererFactory(rf);

      // -- set up processing actions --

      // colors
      ItemAction nodeColor = new NodeColorAction(treeNodes);
      ItemAction textColor = new TextColorAction(treeNodes);
      m_vis.putAction("textColor", textColor);

      ItemAction edgeColor = new ColorAction(treeEdges, VisualItem.STROKECOLOR,
            ColorLib.rgb(200, 200, 200));

      FontAction fonts = new FontAction(treeNodes,
            FontLib.getFont("Tahoma", 10));
      fonts.add("ingroup('_focus_')", FontLib.getFont("Tahoma", 11));

      // recolor
      ActionList recolor = new ActionList(DURATION);
      recolor.add(nodeColor);
      recolor.add(textColor);
      m_vis.putAction("recolor", recolor);

      // repaint
      ActionList repaint = new ActionList(DURATION);
      repaint.add(recolor);
      repaint.add(new RepaintAction());
      m_vis.putAction("repaint", repaint);

      // animate paint change
      ActionList animatePaint = new ActionList(DURATION);
      animatePaint.add(new ColorAnimator(treeNodes));
      animatePaint.add(new RepaintAction());
      m_vis.putAction("animatePaint", animatePaint);

      // create the tree layout action
      treeLayout = new RadialTreeLayout(tree);

      // treeLayout.setAngularBounds(-Math.PI/2, Math.PI);
      m_vis.putAction("treeLayout", treeLayout);

      CollapsedSubtreeLayout subLayout = new CollapsedSubtreeLayout(tree);
      m_vis.putAction("subLayout", subLayout);

      // create the filtering and layout
      ActionList filter = new ActionList(DURATION);
      filter.add(new TreeRootAction(tree));
      filter.add(fonts);
      filter.add(treeLayout);
      filter.add(subLayout);
      filter.add(textColor);
      filter.add(nodeColor);
      filter.add(edgeColor);

      m_vis.putAction("filter", filter);

      // animated transition
      ActionList animate = new ActionList(DURATION);
      animate.setPacingFunction(new SlowInSlowOutPacer());
      animate.add(new QualityControlAnimator());
      animate.add(new VisibilityAnimator(tree));
      animate.add(new PolarLocationAnimator(treeNodes, linear));
      animate.add(new ColorAnimator(treeNodes));
      animate.add(new RepaintAction());

      m_vis.putAction("animate", animate);
      m_vis.alwaysRunAfter("filter", "animate");

      // ------------------------------------------------

      // initialize the display
      setSize(w, h);
      setItemSorter(new TreeDepthItemSorter());
      addControlListener(new DragControl());
      addControlListener(new ZoomToFitControl());
      addControlListener(new ZoomControl());
      addControlListener(new WheelZoomControl());
      addControlListener(new PanControl());
      addControlListener(new FocusControl(1, "filter"));
      addControlListener(new HoverActionControl("repaint"));

      // ------------------------------------------------

      // filter graph and perform layout
      m_vis.run("filter");

   }

   // ------------------------------------------------------------------------

   /**
    * Switch the root of the tree by requesting a new spanning tree at the
    * desired root
    */
   public static class TreeRootAction extends GroupAction {
      public TreeRootAction(String graphGroup) {
         super(graphGroup);
      }

      public void run(double frac) {
         TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
         if (focus == null || focus.getTupleCount() == 0)
            return;

         Graph g = (Graph) m_vis.getGroup(m_group);
         Node f = null;
         Iterator tuples = focus.tuples();
         while (tuples.hasNext() && !g.containsTuple(f = (Node) tuples.next())) {
            f = null;
         }
         if (f == null)
            return;
         g.getSpanningTree(f);
      }
   }

   /**
    * Set node fill colors
    */
   public static class NodeColorAction extends ColorAction {
      public NodeColorAction(String group) {
         super(group, VisualItem.FILLCOLOR, ColorLib.rgba(255, 255, 255, 0));
         add("_hover", ColorLib.gray(220, 230));
         add("ingroup('_focus_')", ColorLib.rgb(198, 229, 229));
      }

   } // end of inner class NodeColorAction

   /**
    * Set node text colors
    */
   public static class TextColorAction extends ColorAction {
      public TextColorAction(String group) {
         super(group, VisualItem.TEXTCOLOR, ColorLib.gray(0));
         add("_hover", ColorLib.rgb(255, 0, 0));
      }
   } // end of inner class TextColorAction

} // end of class RadialGraphView
