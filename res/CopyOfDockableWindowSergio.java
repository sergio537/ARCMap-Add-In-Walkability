import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JButton;
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
import com.esri.arcgis.carto.IActiveView;
import com.esri.arcgis.carto.IMap;
import com.esri.arcgis.carto.LineElement;
import com.esri.arcgis.carto.MapSelection;
import com.esri.arcgis.carto.NetworkLayer;
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
import com.esri.arcgis.geodatabase.IFeatureWorkspaceProxy;
import com.esri.arcgis.geodatabase.IGeometryDef;
import com.esri.arcgis.geodatabase.IGeometryDefEdit;
import com.esri.arcgis.geodatabase.INetworkEdge;
import com.esri.arcgis.geodatabase.INetworkElement;
import com.esri.arcgis.geodatabase.INetworkJunction;
import com.esri.arcgis.geodatabase.INetworkQuery;
import com.esri.arcgis.geodatabase.IWorkspace;
import com.esri.arcgis.geodatabase.IWorkspaceFactory;
import com.esri.arcgis.geodatabase.IWorkspaceName;
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
import com.esri.arcgis.system.IName;
import com.esri.arcgis.system.UID;

public class CopyOfDockableWindowSergio extends DockableWindow {

	private IApplication app;
	private JPanel jPanel;
	//private CompositeGraphicsLayer points;
	//private CompositeGraphicsLayer lines;
	private FeatureLayer lines2;
	private int size;
	//private boolean distanceLayer = false;
	private NetworkLayer networkLayer;
	private IMap map;
	private Set<EdgeInfo> edgeElements;
	private ILineSymbol[] edgeSymbols;
	private ILineSymbol edgeSymbol;
	private DefaultDirectedWeightedGraph<Coord, DefaultWeightedEdge> graph;
	private JTextField textMax;
	private JTextField textLenght;
	private JTextField textAngle;
	private int lenghtBeta = 0;
	private int angleBeta = 0;
	private double maxValue = 0;
	private AlgorithmicColorRamp ramp;
	//List<Coord[]> coordinates = new ArrayList<Coord[]>();
	/*private Fields fields;
	private IFeatureWorkspaceProxy featWork;
	private Set<IPolyline> polylines = new HashSet<IPolyline>();*/
	
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
	
	private class EdgeInfo {
		private LineElement lineElement;
		private Coord cF;
		private Coord cT;
		/*public EdgeInfo(LineElement lineElement, Coord cF, Coord cT) {
			super();
			this.lineElement = lineElement;
			this.cF = cF;
			this.cT = cT;
		}*/
	}

	private IColor getRGBColor(int red, int green, int blue) {
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

	@Override
	public void init(final IApplication app) {
		this.app = app;
		try {
			edgeSymbol = new SimpleLineSymbol();
			edgeSymbol.setColor(getRGBColor(200, 200, 200));
			edgeSymbol.setWidth(1);
			map = (IMap) ((IMxDocument) app.getDocument()).getActiveView();
			for(int i=0; i<map.getLayerCount(); i++) {
				if(map.getLayer(i) instanceof NetworkLayer)
					networkLayer = (NetworkLayer) map.getLayer(i);
			}
			/*points = new CompositeGraphicsLayer();
			points.setName("distances");
			((Map)((IMxDocument)app.getDocument()).getActiveView()).addLayer((ILayer) points);*/
			/*lines = new CompositeGraphicsLayer();
			lines.setName("paths");*/
			IWorkspaceFactory workspaceFactory = new InMemoryWorkspaceFactory();
			IWorkspaceName workspaceName = workspaceFactory.create("", "MyWorkspace", null, 0);
			IWorkspace inmemWor = new IWorkspaceProxy(((IName) workspaceName).open());
			IFeatureWorkspaceProxy featWork = new IFeatureWorkspaceProxy(inmemWor);
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
			field.setName("COST");
			field.setType(esriFieldType.esriFieldTypeDouble);
			fields.addField(field);
			IFeatureClass featureClass = featWork.createFeatureClass("edges", fields, null, new UID(), esriFeatureType.esriFTSimple, "Shape", "");
			lines2.setFeatureClassByRef(featureClass);
			lines2 = new FeatureLayer();
			lines2.setName("paths2");
			map.addLayer(lines2);
			ramp = new AlgorithmicColorRamp();
			ramp.setAlgorithm(esriColorRampAlgorithm.esriHSVAlgorithm);
			IRgbColor fColor = new RgbColor();
			fColor.setRed(255);
			ramp.setFromColor(fColor);
			IRgbColor tColor = new RgbColor();
			tColor.setBlue(255);
			ramp.setToColor(tColor);
			size = 100;
			ramp.setSize(size);
			ramp.createRamp(new boolean[] { true });
			//final IMarkerSymbol[] nodeSymbols = new IMarkerSymbol[size];
			edgeSymbols = new ILineSymbol[size];
			for(int v=0; v<size; v++) {
				/*IMarkerSymbol symbolA = new SimpleMarkerSymbol();
				symbolA.setColor(ramp.getColor(v));
				nodeSymbols[v] = symbolA;*/
				ILineSymbol symbolB = new SimpleLineSymbol();
				symbolB.setColor(ramp.getColor(v));
				symbolB.setWidth(2);
				edgeSymbols[v] = symbolB;
			}
			graph = new DefaultDirectedWeightedGraph<Coord, DefaultWeightedEdge>(DefaultWeightedEdge.class);
			NetworkDataset n = new NetworkDataset(networkLayer.getNetworkDataset());
			INetworkQuery networkQuery = (INetworkQuery)n;
			//IEnumNetworkElement nodes = networkQuery.getElements(esriNetworkElementType.esriNETJunction);
			//final Set<MarkerElement> nodeElements = new HashSet<MarkerElement>();
			/*INetworkJunction node = (INetworkJunction) nodes.next();
			while(node != null) {
				IPoint p = new Point();
				node.queryPoint(p);
				boolean added = graph.addVertex(new Coord(p.getX(),p.getY()));
				if(added) {
					element.setGeometry(p);
					nodeElements.add(element);
					//points.addElement(element, 0);
				}
				node = (INetworkJunction) nodes.next();
			}*/
			IEnumNetworkElement edges = networkQuery.getElements(esriNetworkElementType.esriNETEdge);
			//edgeElements = new HashSet<EdgeInfo>();
			//IFeatureCursor cursor = featureClass.IFeatureClass_insert(true);
			INetworkEdge edge = (INetworkEdge) edges.next();
            while(edge != null) {
				//LineElement elementL = new LineElement();
				INetworkJunction nodeF = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction), nodeT = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction);
				edge.queryJunctions(nodeF, nodeT);
				IPoint pF = new Point();
				nodeF.queryPoint(pF);
				Coord cF = new Coord(pF.getX(),pF.getY());
				graph.addVertex(cF);
				/*MarkerElement element = new MarkerElement();
				if(added) {
					element.setGeometry(pF);
					nodeElements.add(element);
					points.addElement(element, 0);
				}*/
				IPoint pT = new Point();
				nodeT.queryPoint(pT);
				Coord cT = new Coord(pT.getX(),pT.getY());
				graph.addVertex(cT);
				/*element = new MarkerElement();
				if(added) {
					element.setGeometry(pT);
					nodeElements.add(element);
					points.addElement(element, 0);
				}*/
				graph.addEdge(cT, cF);
				DefaultWeightedEdge edgeN = graph.addEdge(cF, cT);
				if(edgeN!=null) {
					IPolyline l = new Polyline();
					l.setFromPoint(pF);
					l.setToPoint(pT);
					//polylines.add(l);
					IFeatureBuffer featureBuffer = featureClass.createFeatureBuffer();
					featureBuffer.setShapeByRef(l);
					features.put(featureBuffer, new Coord[]{cF, cT});
					//cursor.insertFeature(featureBuffer);
				}
				//coordinates.add(new Coord[]{cF, cT});
				INetworkElement e = edges.next();
				while(e != null && !(e instanceof INetworkEdge))
					e = edges.next();
				edge = (INetworkEdge) e;
			}
            JOptionPane.showMessageDialog(null, features.size()+"    gdfs");
            /*cursor.flush();
			lines2.setFeatureClassByRef(featureClass);*/
			((IActiveView) map).refresh();
			JOptionPane.showMessageDialog(jPanel, "Nodes: "+graph.vertexSet().size()+", Links: "+graph.edgeSet().size());
			/*((Map)((IMxDocument)app.getDocument()).getActiveView()).addIActiveViewEventsListener(new IActiveViewEvents() {*/
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
	private void setRenderer(double min, double max, int size, FeatureLayer layer) throws UnknownHostException, IOException {
		ClassBreaksRenderer costRenderer = new ClassBreaksRenderer(); 
		costRenderer.setField("COST");
		costRenderer.setColorRampByRef(ramp);
		costRenderer.setBreakCount(size);
		costRenderer.setExclusionSymbol((SimpleLineSymbol)edgeSymbol);
		costRenderer.setMinimumBreak(min);
		double i=min;
		for(int n=0; n<size; n++)
			try {
				costRenderer.setBreak(n, i+=(max-min)/size);
				costRenderer.setSymbol(n, (ISymbol) edgeSymbols[n]);
			} catch(Exception e) {
				JOptionPane.showMessageDialog(null, "break: "+i+" "+n+" "+min+" "+max+" "+size);
			}
		layer.setRendererByRef(costRenderer);
	}

	/**
	 * Called when the user interface of the dockable window is being initialized.
	 * 
	 * @param component the Swing or AWT component (or container) that constitutes the dockable window's UI
	 * @exception java.io.IOException if there are interop problems.
	 * @exception com.esri.arcgis.interop.AutomationException if the component throws an ArcObjects exception.
	 */
	@Override
	public Component createUI() {
		jPanel = new JPanel();
		jPanel.setLayout(new BorderLayout());
		JPanel jPanelButtons = new JPanel();
		jPanelButtons.setLayout(new GridLayout(3, 1));
		confButtonP(jPanelButtons);
		confButtonN(jPanelButtons);
	    confButtonN2(jPanelButtons);
		jPanel.add(jPanelButtons, BorderLayout.NORTH);
		return jPanel;
	}
	private void confButtonP(JPanel jPanelButtons) {
		final JButton jButton = new JButton("Parametes");
		final JDialog dialog = new JDialog((Frame) jPanelButtons.getParent(), "Paramaters", true);
		JPanel jPanelParams = new JPanel();
		jPanelParams.setLayout(new GridLayout(6, 1));
		jPanelParams.add(new JLabel("Max Cost"));
		NumberFormat format = NumberFormat.getInstance();
	    NumberFormatter formatter = new NumberFormatter(format);
	    formatter.setValueClass(Integer.class);
	    formatter.setMinimum(0);
	    formatter.setMaximum(Integer.MAX_VALUE);
	    formatter.setCommitsOnValidEdit(true);
	    textMax = new JFormattedTextField(formatter);
	    textMax.setText("10");
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

	/*private void confButtonS(final JPanel jPanel) {
		JButton jButton = new JButton("Euclidean");
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(jPanel, "Select one point");
				try {
					JOptionPane.showMessageDialog(jPanel, "0 "+((IMxDocument)app.getDocument()).getActiveView().getClass().toString());
					JOptionPane.showMessageDialog(jPanel, "1 "+((IMxDocument)app.getDocument()).getContentsViewCount());
					JOptionPane.showMessageDialog(jPanel, "2 "+((IMxDocument)app.getDocument()).getCurrentContentsView().getClass().toString());
					JOptionPane.showMessageDialog(jPanel, "3 "+((IMxDocument)app.getDocument()).getContentsView(0).getSelectedItem().getClass().toString());
					JOptionPane.showMessageDialog(jPanel, "4 "+((IMxDocument)app.getDocument()).getPageLayout().getClass().toString());
					JOptionPane.showMessageDialog(jPanel, "5 "+((PageLayout)((IMxDocument)app.getDocument()).getPageLayout()).findFrame(((IMxDocument)app.getDocument()).getActiveView()).getClass().toString());
					JOptionPane.showMessageDialog(jPanel, "6 # "+((FeatureLayer)((IMxDocument)app.getDocument()).getContentsView(0).getSelectedItem()).getSelectionSet().getCount());
				} catch (AutomationException e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getMessage());
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getMessage());
				} catch (NullPointerException e1) {
					JOptionPane.showMessageDialog(jPanel, "null: "+e1.getMessage());
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(jPanel, "other: "+e1.getMessage());
				}
				try {
					NetworkLayer networkLayer = (NetworkLayer) ((IMxDocument)app.getDocument()).getMaps().getItem(0).getLayer(6);
					//networkLayer.getNetworkDataset().
					JOptionPane.showMessageDialog(jPanel, "0 "+networkLayer.getNetworkDataset().getSourceCount());
					JOptionPane.showMessageDialog(jPanel, "1 "+networkLayer.getNetworkDataset().getSourceByID(1).getName()+" "+networkLayer.getNetworkDataset().getSourceByID(1).getSourceType());
					JOptionPane.showMessageDialog(jPanel, "2 "+networkLayer.getNetworkDataset().getSourceByID(2).getName()+" "+networkLayer.getNetworkDataset().getSourceByID(2).getSourceType());
					JOptionPane.showMessageDialog(jPanel, esriNetworkSourceType.esriNSTEdgeFeature+" "+esriNetworkSourceType.esriNSTJunctionFeature+" "+esriNetworkSourceType.esriNSTNetworkSource+" "+esriNetworkSourceType.esriNSTSystemJunction+" "+esriNetworkSourceType.esriNSTTurnFeature);
					NetworkDataset n = new NetworkDataset(networkLayer.getNetworkDataset());
					INetworkQuery networkQuery = (INetworkQuery)n;
					JOptionPane.showMessageDialog(jPanel, "3"); 
					IEnumNetworkElement edges = networkQuery.getElements(esriNetworkElementType.esriNETEdge);
					IEnumNetworkElement nodes = networkQuery.getElements(esriNetworkElementType.esriNETJunction);
					JOptionPane.showMessageDialog(jPanel, "4 "+networkQuery.getElementCount(esriNetworkElementType.esriNETEdge)+" "+networkQuery.getElementCount(esriNetworkElementType.esriNETJunction)+" "+networkQuery.getElementCount(esriNetworkElementType.esriNETTurn)); 
					NetworkEdge edge = (NetworkEdge)edges.next();
					if(edge!=null) {
						INetworkJunction fromJunction = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction), toJunction = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction);
						edge.queryJunctions(fromJunction, toJunction);
						JOptionPane.showMessageDialog(jPanel, "5");
						Point pointF = new Point(), pointT = new Point();
						fromJunction.queryPoint(pointF);
						toJunction.queryPoint(pointT);
						JOptionPane.showMessageDialog(jPanel, pointF.getX()+" "+pointF.getY()+" -- "+pointT.getX()+" "+pointT.getY());
						edge = (NetworkEdge)edges.next();
					}
					else {
						JOptionPane.showMessageDialog(jPanel, "6"); 
						NetworkJunction node = (NetworkJunction)nodes.next();
						while(node!=null) {
							node = (NetworkJunction)nodes.next();
						}
					}
					//((EdgeFeatureSource)networkLayer.getNetworkDataset().getSourceByID(1)).;
					//Network
				} catch (AutomationException e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getMessage());
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getMessage());
				} catch (NullPointerException e1) {
					JOptionPane.showMessageDialog(jPanel, "null: "+e1.getMessage());
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(jPanel, "other: "+e1.getMessage());
				}
			}
		});
		jPanel.add(jButton);
	}*/
	/*private void confButtonH(final JPanel jPanel) {
		JButton jButton = new JButton("Height");
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					ILayer layer = ((IMxDocument)app.getDocument()).getMaps().getItem(0).getLayer(0);
					IFeatureClass table = ((IFeatureLayer)layer).getFeatureClass();
					if(table.findField("COLOR")<0){
						IField color = new Field();
						((Field)color).setType(esriFieldType.esriFieldTypeDouble);
						((Field)color).setName("COLOR");
						table.addField(color);
					}
					IFeatureCursor cursor = table.search(null, true);
					IFeature feature = cursor.nextFeature();
					while(feature!=null) {
						feature.setValue(table.findField("COLOR"), ((IPolygon)feature.getShape()).getEnvelope().getHeight());
						feature.store();
						feature = cursor.nextFeature();
					}
					ICursor cursor2 = ((FeatureLayer)layer).ITable_search(null, true);
					DataStatistics dataStatistics = new DataStatistics();
					dataStatistics.setCursorByRef(cursor2);
					dataStatistics.setField("COLOR");
					IStatisticsResults statisticsResult = dataStatistics.getStatistics();
					UniqueValueRenderer renderer = new UniqueValueRenderer();
					renderer.setFieldCount(1);
					renderer.setField(0, "COLOR");
					IAlgorithmicColorRamp ramp = new AlgorithmicColorRamp();
					ramp.setAlgorithm(esriColorRampAlgorithm.esriHSVAlgorithm);
					IRgbColor fColor = new RgbColor();
					fColor.setRed(255);
					ramp.setFromColor(fColor);
					IRgbColor tColor = new RgbColor();
					tColor.setBlue(255);
					ramp.setToColor(tColor);
					int size = 10000;
					ramp.setSize(size);
					ramp.createRamp(new boolean[]{true});
					cursor = table.search(null, true);
					feature = cursor.nextFeature();
					while(feature!=null) {
						double v = (Double) feature.getValue(table.findField("COLOR"));
						SimpleFillSymbol symbol = new SimpleFillSymbol();
						symbol.setColor(ramp.getColor((int) ((size-1)*(v-statisticsResult.getMinimum())/(statisticsResult.getMaximum()-statisticsResult.getMinimum()))));
						renderer.addValue(v+"", v+"", symbol);
						feature = cursor.nextFeature();
					}
					((FeatureLayer)layer).setRendererByRef(renderer);
					((IMxDocument)app.getDocument()).getActiveView().refresh();
					JOptionPane.showMessageDialog(jPanel, "Done");
				} catch (AutomationException e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getMessage());
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getMessage());
				}
			}
		});
		jPanel.add(jButton);
	}
	private void confButtonV(final JPanel jPanel) {
		JButton jButton2 = new JButton("Vertical");
		jButton2.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
					ILayer layer = ((IMxDocument)app.getDocument()).getMaps().getItem(0).getLayer(0);
					double yMax = layer.getAreaOfInterest().getYMax();
					double yMin = layer.getAreaOfInterest().getYMin();
					IFeatureClass table = ((IFeatureLayer)layer).getFeatureClass();
					if(table.findField("COLOR")<0){
						IField color = new Field();
						((Field)color).setType(esriFieldType.esriFieldTypeInteger);
						((Field)color).setName("COLOR");
						table.addField(color);
					}
					UniqueValueRenderer renderer = new UniqueValueRenderer();
					renderer.setFieldCount(1);
					renderer.setField(0, "COLOR");
					IAlgorithmicColorRamp ramp = new AlgorithmicColorRamp();
					ramp.setAlgorithm(esriColorRampAlgorithm.esriHSVAlgorithm);
					IRgbColor fColor = new RgbColor();
					fColor.setRed(255);
					ramp.setFromColor(fColor);
					IRgbColor tColor = new RgbColor();
					tColor.setBlue(255);
					ramp.setToColor(tColor);
					int size = 256;
					ramp.setSize(size);
					ramp.createRamp(new boolean[]{true});
					IFeatureCursor cursor = table.search(null, true);
					IFeature feature = cursor.nextFeature();
					while(feature!=null) {
						int v = new Integer((int)((((IPolygon)feature.getShape()).getEnvelope().getYMax()-yMin)*(size-1)/(yMax-yMin)));
						feature.setValue(table.findField("COLOR"), v);
						feature.store();
						SimpleFillSymbol symbol = new SimpleFillSymbol();
						symbol.setColor(ramp.getColor(v));
						renderer.addValue(v+"", v+"", symbol);
						feature = cursor.nextFeature();
					}
					((FeatureLayer)layer).setRendererByRef(renderer);
					((IMxDocument)app.getDocument()).getActiveView().refresh();
					JOptionPane.showMessageDialog(jPanel, "Done");
				} catch (AutomationException e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getMessage());
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getMessage());
				}
			}
		});
		jPanel.add(jButton2);
	}*/
	private void confButtonN(final JPanel jPanel) {
		JButton jButton = new JButton("Replacing colors");
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (map.getSelectionCount() == 1)
						if(!textMax.getText().isEmpty() && !textLenght.getText().isEmpty() && !textAngle.getText().isEmpty()) {
							long[] times = new long[7];
							MapSelection mapSelection = new MapSelection(map.getFeatureSelection());
							Point feature = (Point)mapSelection.next().getShape();
							Coord c = new Coord(feature.getX(), feature.getY());
							times[6] = System.currentTimeMillis();
							BellmanFordShortestPath<Coord, DefaultWeightedEdge> algo = new BellmanFordShortestPath<Coord, DefaultWeightedEdge>(graph, c);
							times[6] = System.currentTimeMillis()-times[6];
							double max = new Double(textMax.getText());
							int lenghtB = new Integer(textLenght.getText());
							int angleB = new Integer(textAngle.getText());
							if(lenghtB!=lenghtBeta || angleB!=angleBeta) {
								times[4] = System.currentTimeMillis();
								for(DefaultWeightedEdge edge:graph.edgeSet())
									graph.setEdgeWeight(edge, getCost(lenghtB, angleB, edge));
								times[4] = System.currentTimeMillis()-times[4];
								lenghtBeta = lenghtB;
								angleBeta = angleB;
							}
							times[5] = System.currentTimeMillis();
							for(EdgeInfo element:edgeElements) {
								times[0] -= System.currentTimeMillis();
								double fCost = element.cF.equals(c)?0:algo.getCost(element.cF);
								times[0] += System.currentTimeMillis();
								times[1] -= System.currentTimeMillis();
								double tCost = element.cT.equals(c)?0:algo.getCost(element.cT);
								times[1] += System.currentTimeMillis();
								double cost = (fCost+tCost)/2;
								times[2] -= System.currentTimeMillis();
								if(cost<max) {
									int v = new Integer((int) ((cost - 0) * (size - 1) / (max - 0)));
									element.lineElement.setSymbol(edgeSymbols[v]);
								}
								else
									element.lineElement.setSymbol(edgeSymbol);
								times[2] += System.currentTimeMillis();
							}
							times[5] = System.currentTimeMillis()-times[5];
							((IMxDocument) app.getDocument()).getActiveView().refresh();
							JOptionPane.showMessageDialog(jPanel, times[0]/edgeElements.size()+" "+times[1]/edgeElements.size()+" "+times[2]/edgeElements.size()+" "+times[3]/edgeElements.size()+" "+times[4]+" "+times[5]+" "+times[6]);
						}
						else
							JOptionPane.showMessageDialog(jPanel, "Fill the numeric parameters");	
					else
						JOptionPane.showMessageDialog(jPanel, "Select just one node");
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getStackTrace()[0].getMethodName()+" "+e1.getStackTrace()[0].getLineNumber()+" "+e1.getStackTrace()[0].getClassName()+" "+e1.getMessage()+" "+e1.getClass().toString());
				}

			}

			private double getCost(int lenghtB, int angleB, DefaultWeightedEdge edge) {
				Coord s = graph.getEdgeSource(edge);
				Coord t = graph.getEdgeTarget(edge);
				return (lenghtB*Math.hypot(s.x-t.x,s.y-t.y)/300+angleB*(Math.atan2(t.y-s.y, t.x-s.x)+Math.PI)/(2*Math.PI))/(lenghtB+angleB);
			}
		});
		jPanel.add(jButton);
	}
	private void confButtonN2(final JPanel jPanel) {
		JButton jButton = new JButton("Cursor");
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (map.getSelectionCount() == 1)
						if(!textMax.getText().isEmpty() && !textLenght.getText().isEmpty() && !textAngle.getText().isEmpty()) {
							long[] times = new long[7];
							MapSelection mapSelection = new MapSelection(map.getFeatureSelection());
							Point feature = (Point)mapSelection.next().getShape();
							Coord c = new Coord(feature.getX(), feature.getY());
							int lenghtB = new Integer(textLenght.getText());
							int angleB = new Integer(textAngle.getText());
							if(lenghtB!=lenghtBeta || angleB!=angleBeta) {
								times[4] = System.currentTimeMillis();
								for(DefaultWeightedEdge edge:graph.edgeSet())
									graph.setEdgeWeight(edge, getCost(lenghtB, angleB, edge));
								lines2.deleteSearchedRows(null);
								map.deleteLayer(lines2);
								times[4] = System.currentTimeMillis()-times[4];
								lenghtBeta = lenghtB;
								angleBeta = angleB;
							}
							times[5] = System.currentTimeMillis();
							BellmanFordShortestPath<Coord, DefaultWeightedEdge> algo = new BellmanFordShortestPath<Coord, DefaultWeightedEdge>(graph, c);
							//IFeatureCursor cursor = lines2.getFeatureClass().IFeatureClass_insert(true);
							int num = 0;
							for (Entry<IFeatureBuffer, Coord[]> entry:features.entrySet()) {
								times[0] -= System.currentTimeMillis();
								//Coord[] coords = coordinates.get(num);
								Coord[] coords = entry.getValue();
								Coord cF = coords[0];
								Coord cT = coords[1];
								times[0] += System.currentTimeMillis();
								times[1] -= System.currentTimeMillis();
								double fCost = cF.equals(c)?0:algo.getCost(cF);
								double tCost = cT.equals(c)?0:algo.getCost(cT);
								times[1] += System.currentTimeMillis();
								double cost = (fCost+tCost)/2;
								times[2] -= System.currentTimeMillis();
								entry.getKey().setValue(1, cost);
								times[2] += System.currentTimeMillis();
								times[3] -= System.currentTimeMillis();
								lines2.add((IFeature) entry.getKey());
								times[3] += System.currentTimeMillis();
								//feature2 = cursor.nextFeature();
								num++;
							}
							JOptionPane.showMessageDialog(jPanel, num);
							map.addLayer(lines2);
							JOptionPane.showMessageDialog(jPanel, num+1);
							//cursor.flush();
							times[5] = System.currentTimeMillis()-times[5];
							double max = new Double(textMax.getText());
							if(max!=maxValue) {
								times[6] = System.currentTimeMillis();
								setRenderer(0, max, size, lines2);
								maxValue = max;
								times[6] = System.currentTimeMillis()-times[3];
							}
							((IMxDocument) app.getDocument()).getActiveView().refresh();
							JOptionPane.showMessageDialog(jPanel, times[0]/num+" "+times[1]/num+" "+times[2]/num+" "+times[3]/num+" "+times[4]+" "+times[5]+" "+times[6]);
						}
						else
							JOptionPane.showMessageDialog(jPanel, "Fill the numeric parameters");	
					else
						JOptionPane.showMessageDialog(jPanel, "Select just one node");
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getStackTrace()[0].getMethodName()+" "+e1.getStackTrace()[0].getLineNumber()+" "+e1.getStackTrace()[0].getClassName()+" "+e1.getMessage()+" "+e1.getClass().toString());
				}

			}

			private double getCost(int lenghtB, int angleB, DefaultWeightedEdge edge) {
				Coord s = graph.getEdgeSource(edge);
				Coord t = graph.getEdgeTarget(edge);
				return (lenghtB*Math.hypot(s.x-t.x,s.y-t.y)/300+angleB*(Math.atan2(t.y-s.y, t.x-s.x)+Math.PI)/(2*Math.PI))/(lenghtB+angleB);
			}
		});
		jPanel.add(jButton);
	}

}
