import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.NumberFormatter;

import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.esri.arcgis.addins.desktop.DockableWindow;
import com.esri.arcgis.arcmapui.IMxDocument;
import com.esri.arcgis.carto.ClassBreaksRenderer;
import com.esri.arcgis.carto.FeatureLayer;
import com.esri.arcgis.carto.Map;
import com.esri.arcgis.carto.MapSelection;
import com.esri.arcgis.carto.NetworkLayer;
import com.esri.arcgis.carto.esriViewDrawPhase;
import com.esri.arcgis.datasourcesGDB.InMemoryWorkspaceFactory;
import com.esri.arcgis.display.AlgorithmicColorRamp;
import com.esri.arcgis.display.IColor;
import com.esri.arcgis.display.ILineSymbol;
import com.esri.arcgis.display.IRgbColor;
import com.esri.arcgis.display.ISymbol;
import com.esri.arcgis.display.RgbColor;
import com.esri.arcgis.display.SimpleLineSymbol;
import com.esri.arcgis.display.esriColorRampAlgorithm;
import com.esri.arcgis.framework.IApplication;
import com.esri.arcgis.geodatabase.Field;
import com.esri.arcgis.geodatabase.Fields;
import com.esri.arcgis.geodatabase.GeometryDef;
import com.esri.arcgis.geodatabase.IEnumNetworkElement;
import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geodatabase.IFeatureBuffer;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.IFeatureWorkspaceProxy;
import com.esri.arcgis.geodatabase.IGeometryDef;
import com.esri.arcgis.geodatabase.IGeometryDefEdit;
import com.esri.arcgis.geodatabase.INetworkEdge;
import com.esri.arcgis.geodatabase.INetworkElement;
import com.esri.arcgis.geodatabase.INetworkJunction;
import com.esri.arcgis.geodatabase.INetworkQuery;
import com.esri.arcgis.geodatabase.IWorkspaceFactory;
import com.esri.arcgis.geodatabase.IWorkspaceProxy;
import com.esri.arcgis.geodatabase.NetworkDataset;
import com.esri.arcgis.geodatabase.esriFeatureType;
import com.esri.arcgis.geodatabase.esriFieldType;
import com.esri.arcgis.geodatabase.esriNetworkElementType;
import com.esri.arcgis.geometry.IPoint;
import com.esri.arcgis.geometry.IPolyline;
import com.esri.arcgis.geometry.Point;
import com.esri.arcgis.geometry.Polyline;
import com.esri.arcgis.geometry.esriGeometryType;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.system.Cleaner;
import com.esri.arcgis.system.IName;
import com.esri.arcgis.system.UID;

/**
 * Test class that represents the dockable window for ArcMap
 * @author sergioo
 *
 */
public class DockableWindowSergio extends DockableWindow {

	private static final int SIZE = 10;
	
	private JPanel jPanel;
	private FeatureLayer roadsCostLayer;
	private NetworkLayer networkLayer;
	private Map map;
	private ILineSymbol[] edgeSymbols;
	private ILineSymbol exclusionSymbol;
	private DefaultDirectedWeightedGraph<Coord, DefaultWeightedEdge> graph;
	private JTextField textMax;
	private JTextField textLenght;
	private JTextField textAngle;
	private JCheckBox chCross;
	private int lenghtBeta = 0;
	private int angleBeta = 0;
	private Collection<RoadInfo> roadsInfo = new ArrayList<RoadInfo>();
	private AlgorithmicColorRamp ramp;
	private List<EdgeInfo> edgesInfo = new ArrayList<EdgeInfo>();
	private String[] crosss;
	private double max = 0;
	
	/**
	 * Class to represent a coordinate
	 * @author sergioo
	 *
	 */
	private static class Coord {
		private double x;
		private double y;

		public Coord(double x, double y) {
			super();
			this.x = x;
			this.y = y;
		}

		@Override
		public int hashCode() {
			return (int) (x*y);
		}
		
		@Override
		public String toString() {
			return x+" , "+y;
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof Coord)
				if(((Coord)o).x==x && ((Coord)o).y==y)
					return true;
			return false;
		}
	}
	/**
	 * Class to represent geometry and graphic object of each road
	 * @author sergioo
	 *
	 */
	private class RoadInfo {
		private Coord cF;
		private Coord cT;
		private IPolyline p;
		public RoadInfo(Coord cF, Coord cT, IPolyline p) {
			super();
			this.cF = cF;
			this.cT = cT;
			this.p = p;
		}
	}
	/**
	 * Class to save extra information of each edge
	 * @author sergioo
	 *
	 */
	private class EdgeInfo {
		private DefaultWeightedEdge edge;
		private String crossing;
		public EdgeInfo(DefaultWeightedEdge edge, String crossing) {
			super();
			this.edge = edge;
			this.crossing = crossing;
		}
	}
	/**
	 * Function to return the IColor object given the red, green and blue integers
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	private static IColor getRGBColor(int red, int green, int blue) {
		RgbColor rgbColor = null;
		try {
			rgbColor = new RgbColor();
			rgbColor.setRed(red);
			rgbColor.setGreen(green);
			rgbColor.setBlue(blue);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rgbColor;
	}
	/**
	 * Method to initialize the window
	 */
	@Override
	public void init(final IApplication app) {
		try {
			//Precalculate color symbols for the edges
			exclusionSymbol = new SimpleLineSymbol();
			exclusionSymbol.setColor(getRGBColor(200, 200, 200));
			exclusionSymbol.setWidth(1);
			ramp = new AlgorithmicColorRamp();
			ramp.setAlgorithm(esriColorRampAlgorithm.esriHSVAlgorithm);
			IRgbColor fColor = new RgbColor();
			fColor.setRed(255);
			ramp.setFromColor(fColor);
			IRgbColor tColor = new RgbColor();
			tColor.setBlue(255);
			ramp.setToColor(tColor);
			ramp.setSize(SIZE);
			ramp.createRamp(new boolean[] { true });
			edgeSymbols = new ILineSymbol[SIZE];
			for(int v=0; v<SIZE; v++) {
				ILineSymbol symbolB = new SimpleLineSymbol();
				symbolB.setColor(ramp.getColor(v));
				symbolB.setWidth(1.5);
				edgeSymbols[v] = symbolB;
			}
			//Save the main map
			map = (Map) ((IMxDocument) app.getDocument()).getActiveView();
			//Define the network layer and the roads graphic layer (this should be done by the user)
			FeatureLayer roadsLayer = null;
			for(int i=0; i<map.getLayerCount(); i++) {
				if(map.getLayer(i) instanceof NetworkLayer)
					networkLayer = (NetworkLayer) map.getLayer(i);
				else if(map.getLayer(i).getName().equals("osm_ped_in_singapore"))
					roadsLayer = (FeatureLayer) map.getLayer(i);
			}
			//Save the road types in an auxiliary array
			IFeatureCursor cursor = roadsLayer.getFeatureClass().search(null, true);
			IFeature feature = cursor.nextFeature();
			int size=0;
			while(feature!=null) {
				if(feature.getOID()>size)
					size=feature.getOID();
				feature = cursor.nextFeature();
			}
			cursor.flush();
			cursor = roadsLayer.getFeatureClass().search(null, true);
			feature = cursor.nextFeature();
			int functionIndex = roadsLayer.getFeatureClass().findField("highway");
			crosss = new String[size+1];
			while(feature!=null) {
				crosss[feature.getOID()] = (String) feature.getValue(functionIndex);
				feature = cursor.nextFeature();
			}
			cursor.flush();
			//Create the graph object
			graph = new DefaultDirectedWeightedGraph<Coord, DefaultWeightedEdge>(DefaultWeightedEdge.class);
			//Create an in-memory workspace to create a new feature class to represent roads with a cost
			IWorkspaceFactory workspaceFactory = new InMemoryWorkspaceFactory();
			IFeatureWorkspaceProxy featWork = new IFeatureWorkspaceProxy(new IWorkspaceProxy(((IName) workspaceFactory.create("", "MyWorkspace", null, 0)).open()));
			Fields fields = new Fields();
			Field gField = new Field();
			IGeometryDef geometryDef = new GeometryDef();
			IGeometryDefEdit geometryDefEdit = (IGeometryDefEdit)geometryDef;
			geometryDefEdit.setGeometryType(esriGeometryType.esriGeometryPolyline);
			geometryDefEdit.setSpatialReferenceByRef(networkLayer.getSpatialReference()); 
			gField.setName("Shape");
			gField.setType(esriFieldType.esriFieldTypeGeometry);
			gField.setGeometryDefByRef(geometryDefEdit);
			fields.addField(gField);
			Field field = new Field();
			field.setName("Distance");
			field.setType(esriFieldType.esriFieldTypeDouble);
			fields.addField(field);
			IFeatureClass featureClass = featWork.createFeatureClass("edges", fields, null, new UID(), esriFeatureType.esriFTSimple, "Shape", "");
			//Cursor to insert each road as a feature
			cursor = featureClass.IFeatureClass_insert(true);
			IFeatureBuffer featureBuffer = featureClass.createFeatureBuffer();
			NetworkDataset n = new NetworkDataset(networkLayer.getNetworkDataset());
			INetworkQuery networkQuery = (INetworkQuery)n;
			IEnumNetworkElement edges = networkQuery.getElements(esriNetworkElementType.esriNETEdge);
			INetworkEdge edge = (INetworkEdge) edges.next();
			//In this loop vertexes and edges are added to the graph and features are added to the new feature class 
			while(edge != null) {
				INetworkJunction nodeF = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction), nodeT = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction);
				edge.queryJunctions(nodeF, nodeT);
				IPoint pF = new Point();
				nodeF.queryPoint(pF);
				Coord cF = new Coord(pF.getX(),pF.getY());
				graph.addVertex(cF);
				IPoint pT = new Point();
				nodeT.queryPoint(pT);
				Coord cT = new Coord(pT.getX(),pT.getY());
				graph.addVertex(cT);
				DefaultWeightedEdge edgeN = graph.addEdge(cT, cF);
				if(edgeN!=null)
					edgesInfo.add(new EdgeInfo(edgeN, crosss[edge.getOID()]));
				edgeN = graph.addEdge(cF, cT);
				if(edgeN!=null && Math.random()<2) {
					edgesInfo.add(new EdgeInfo(edgeN, crosss[edge.getOID()]));
					IPolyline p = new Polyline();
					p.setFromPoint(pF);
					p.setToPoint(pT);
					roadsInfo.add(new RoadInfo(cF, cT, p));
					featureBuffer.setShapeByRef(p);
					cursor.insertFeature(featureBuffer);
				}
				INetworkElement e = edges.next();
				while(e != null && !(e instanceof INetworkEdge))
					e = edges.next();
				edge = (INetworkEdge) e;
			}
			//New layer is created and added to the map
			roadsCostLayer = new FeatureLayer();
			roadsCostLayer.setName("roads");
			roadsCostLayer.setFeatureClassByRef(featureClass);
			map.addLayer(roadsCostLayer);
			map.refresh();
			JOptionPane.showMessageDialog(jPanel, "Nodes: "+graph.vertexSet().size()+", Links: "+graph.edgeSet().size());
		} catch (AutomationException e1) {
			for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());
			JOptionPane.showMessageDialog(null, e1.getMessage());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		} catch (NullPointerException e1) {
			for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());
			JOptionPane.showMessageDialog(null, "null: "+e1.getMessage());
		} catch (Exception e1) {
			for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());
			JOptionPane.showMessageDialog(null, "other: "+e1.getClass().toString()+" "+e1.getMessage());
		}
	}
	/**
	 * GUI creation and configuration
	 * @param jPanel
	 */
	@Override
	public Component createUI() {
		jPanel = new JPanel();
		jPanel.setLayout(new BorderLayout());
		JPanel jPanelButtons = new JPanel();
		jPanelButtons.setLayout(new GridLayout(2, 1));
		confButtonP(jPanelButtons);
	    confButtonN(jPanelButtons);
		jPanel.add(jPanelButtons, BorderLayout.NORTH);
		return jPanel;
	}
	/**
	 * Configuration of the first button
	 * @param jPanel
	 */
	private void confButtonP(JPanel jPanelButtons) {
		final JButton jButton = new JButton("Parameters");
		final JDialog dialog = new JDialog((Frame) jPanelButtons.getParent(), "Paramaters", true);
		JPanel jPanelParams = new JPanel();
		jPanelParams.setLayout(new GridLayout(7, 1));
		jPanelParams.add(new JLabel("Max Cost"));
		NumberFormat format = NumberFormat.getInstance();
	    NumberFormatter formatter = new NumberFormatter(format);
	    formatter.setValueClass(Integer.class);
	    formatter.setMinimum(0);
	    formatter.setMaximum(Integer.MAX_VALUE);
	    formatter.setCommitsOnValidEdit(true);
	    textMax = new JFormattedTextField(formatter);
	    textMax.setText("1500");
	    textMax.setEditable(true);
	    textMax.setEnabled(true);
	    jPanelParams.add(textMax);
	    jPanelParams.add(new JLabel("Lenght 1-10"));
	    textLenght = new JFormattedTextField(formatter);
	    textLenght.setText("10");
	    textLenght.setEditable(true);
	    textLenght.setEnabled(true);
	    jPanelParams.add(textLenght);
	    jPanelParams.add(new JLabel("Angle 1-10"));
	    textAngle = new JFormattedTextField(formatter);
	    textAngle.setText("0");
	    textAngle.setEditable(true);
	    textAngle.setEnabled(true);
	    jPanelParams.add(textAngle);
	    chCross = new JCheckBox();
	    chCross.setText("Crossings");
	    chCross.setEnabled(true);
	    jPanelParams.add(chCross);
	    dialog.setLayout(new BorderLayout());
	    dialog.add(jPanelParams, BorderLayout.CENTER);
	    dialog.pack();
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setLocationRelativeTo(jButton);
				dialog.setVisible(true);
			}
		});
	    jPanelButtons.add(jButton);
	}
	/**
	 * Configuration of the second button
	 * @param jPanel
	 */
	private void confButtonN(final JPanel jPanel) {
		JButton jButton = new JButton("Walking paths");
		jButton.addActionListener(new ActionListener() {
			
			/**
			 * Method executed when second button is clicked
			 */
			public void actionPerformed(ActionEvent e) {
				try {
					if (map.getSelectionCount() == 1)
						if(!textMax.getText().isEmpty() && !textLenght.getText().isEmpty() && !textAngle.getText().isEmpty()) {
							long[] times = new long[8];
							JOptionPane.showMessageDialog(jPanel, roadsInfo.size()+"-"+graph.edgeSet().size()+" links to update");
							times[7] -= System.currentTimeMillis();
							MapSelection mapSelection = new MapSelection(map.getFeatureSelection());
							Point feature = (Point)mapSelection.next().getShape();
							Coord c = new Coord(feature.getX(), feature.getY());
							int lenghtB = new Integer(textLenght.getText());
							int angleB = new Integer(textAngle.getText());
							if(lenghtB!=lenghtBeta || angleB!=angleBeta) {
								times[0] -= System.currentTimeMillis();
								for(EdgeInfo edgeInfo:edgesInfo)
									graph.setEdgeWeight(edgeInfo.edge, getCost(lenghtB, angleB, edgeInfo.edge, edgeInfo.crossing));
								times[0] += System.currentTimeMillis();
								lenghtBeta = lenghtB;
								angleBeta = angleB;
							}
							//Run shortest path algorithm 1 to n
							times[1] -= System.currentTimeMillis();
							BellmanFordShortestPath<Coord, DefaultWeightedEdge> algo = new BellmanFordShortestPath<Coord, DefaultWeightedEdge>(graph, c);
							algo.getCost(graph.vertexSet().iterator().next());
							times[1] += System.currentTimeMillis();
							//Set the renderer if there is any change
							double m = new Double(textMax.getText());
							if(m!=max) {
								max = m;
								times[2] -= System.currentTimeMillis();
								setRoadsRenderer(0, max);
								times[2] += System.currentTimeMillis();
								map.refresh();
							}
							//Delete all rows of the roads layer
							roadsCostLayer.deleteSearchedRows(null);
							//Obtain the current table (feature class), find the cost field and create a feature buffer
							IFeatureClass featureClass = roadsCostLayer.getFeatureClass();
							int costIndex = featureClass.findField("Distance"); 
							IFeatureBuffer featureBuffer = featureClass.createFeatureBuffer();
							//For each road insert a row in the road feature layer wit an updated cost
							IFeatureCursor cursor = featureClass.IFeatureClass_insert(true);
							times[3] -= System.currentTimeMillis();
							int num = 0;
							for(RoadInfo roadInfo:roadsInfo) {
								Coord cF = roadInfo.cF;
								Coord cT = roadInfo.cT;
								//times[4] -= System.currentTimeMillis();
								double fCost = cF.equals(c)?0:algo.getCost(cF);
								double tCost = cT.equals(c)?0:algo.getCost(cT);
								//times[4] += System.currentTimeMillis();
								double cost = (fCost+tCost)/2;
								//times[5] -= System.currentTimeMillis();
								featureBuffer.setShapeByRef(roadInfo.p);
								featureBuffer.setValue(costIndex, cost);
								//times[5] += System.currentTimeMillis();
								//times[6] -= System.currentTimeMillis();
								cursor.insertFeature(featureBuffer);
								//times[6] += System.currentTimeMillis();
								//Each 100 inserted elements flush the cursor and refresh the active view (map).
								if(++num%100==0) {
									cursor.flush();
									map.partialRefresh(esriViewDrawPhase.esriViewAll, null, null);
								}
							}
							cursor.flush();
							Cleaner.release(cursor);
							map.refresh();
							times[3] += System.currentTimeMillis();
							times[7] += System.currentTimeMillis();
							JOptionPane.showMessageDialog(jPanel, "costs:"+times[0]+", paths:"+times[1]+", renderer:"+times[2]+", insertion:"+times[3]/*+", total:"+times[7]*/);
						}
						else
							JOptionPane.showMessageDialog(jPanel, "Fill the numeric parameters");
					else
						JOptionPane.showMessageDialog(jPanel, "Select just one node");
				} catch (Exception e1) {
					for(StackTraceElement trace:e1.getStackTrace())
						JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());
					JOptionPane.showMessageDialog(null, e1.getMessage());
				}

			}

			private double getCost(int lenghtB, int angleB, DefaultWeightedEdge edge, String crossing) throws AutomationException, IOException {
				Coord s = graph.getEdgeSource(edge);
				Coord t = graph.getEdgeTarget(edge);
				double addDistance = chCross.isSelected()?(!crossing.equals("footway")?100:0):0;
				return (lenghtB*(addDistance + Math.hypot(s.x-t.x,s.y-t.y))+angleB*300*(Math.sin(Math.atan2(t.y-s.y, t.x-s.x))+1)/2)/(lenghtB+angleB);
			}
		});
		jPanel.add(jButton);
	}
	/**
	 * Method to set a new renderer to the roads when user changes conditions
	 * @param min
	 * @param max
	 * @param size
	 * @param roadsCostLayer
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void setRoadsRenderer(double min, double max) throws UnknownHostException, IOException {
		ClassBreaksRenderer costRenderer = new ClassBreaksRenderer(); 
		costRenderer.setField("Distance");
		costRenderer.setBreakCount(SIZE);
		costRenderer.setExclusionSymbol((SimpleLineSymbol)exclusionSymbol);
		costRenderer.setMinimumBreak(min);
		costRenderer.setColorRampByRef(ramp);
		costRenderer.setShowExclusionClass(true);
		double i=min;
		for(int n=0; n<SIZE; n++)
			try {
				costRenderer.setBreak(n, i+=(max-min)/SIZE);
				costRenderer.setSymbol(n, (ISymbol) edgeSymbols[n]);
			} catch(Exception e) {
				JOptionPane.showMessageDialog(null, "break: "+i+" "+n+" "+min+" "+max+" "+SIZE);
			}
		roadsCostLayer.setRendererByRef(costRenderer);
	}
	/**
	 * Method to set a new renderer to the nodes when user changes conditions
	 * @param layer
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	/*private void setRenderer2(FeatureLayer layer) throws UnknownHostException, IOException {
		ProportionalSymbolRenderer sizeRenderer = new ProportionalSymbolRenderer();
		sizeRenderer.setField("PARCEL_SIZ");
		sizeRenderer.setLegendSymbolCount(5);
		
		double i=min;
		for(int n=0; n<size; n++)
			try {
				costRenderer.setBreak(n, i+=(max-min)/size);
				costRenderer.setSymbol(n, (ISymbol) edgeSymbols[n]);
			} catch(Exception e) {
				JOptionPane.showMessageDialog(null, "break: "+i+" "+n+" "+min+" "+max+" "+size);
			}
		layer.setRendererByRef(costRenderer);
	}*/

}
