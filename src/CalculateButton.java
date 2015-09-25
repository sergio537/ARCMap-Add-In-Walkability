import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.esri.arcgis.addins.desktop.Button;
import com.esri.arcgis.arcmapui.IMxDocument;
import com.esri.arcgis.carto.BiUniqueValueRenderer;
import com.esri.arcgis.carto.ClassBreaksRenderer;
import com.esri.arcgis.carto.FeatureLayer;
import com.esri.arcgis.carto.Map;
import com.esri.arcgis.carto.UniqueValueRenderer;
import com.esri.arcgis.display.ILineSymbol;
import com.esri.arcgis.display.IMarkerSymbol;
import com.esri.arcgis.display.ISymbol;
import com.esri.arcgis.display.SimpleFillSymbol;
import com.esri.arcgis.display.SimpleLineSymbol;
import com.esri.arcgis.display.SimpleMarkerSymbol;
import com.esri.arcgis.framework.IApplication;
import com.esri.arcgis.geodatabase.FeatureCursor;
import com.esri.arcgis.geodatabase.ICursor;
import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geodatabase.IFeatureBuffer;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.ISelectionSet;
import com.esri.arcgis.geometry.IArea;
import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.IPoint;
import com.esri.arcgis.geometry.IPolygon;
import com.esri.arcgis.geometry.Multipoint;
import com.esri.arcgis.geometry.Point;
import com.esri.arcgis.geometry.Polygon;
import com.esri.arcgis.geometry.Polyline;
import com.esri.arcgis.interop.AutomationException;


public class CalculateButton extends Button {

	private static final int SIZE_CIRCLE = 300;
	public static HashMap<Integer, RoadInfo> roadsInfo = new HashMap<Integer, RoadInfo>();
	public static SimpleLineSymbol exclusionSymbol;
	public static SimpleFillSymbol polygonSymbol1;
	public static SimpleFillSymbol polygonSymbol2;
	public static SimpleFillSymbol circleSymbol;
	public static SimpleMarkerSymbol exclusionSymbolL;
	public static IMarkerSymbol[] locationSymbolsC;
	public static IMarkerSymbol[] locationSymbolsS;
	public static ILineSymbol[] edgeSymbols;
	private IApplication app;
	public static boolean isEnabled = false;
	
	@Override
	public void init(IApplication app) throws AutomationException, IOException {
	    this.app = app;
	}
	/**
	 * Class to represent a node of the graph
	 * @author sergioo
	 *
	 */
	public static class NodeCoord {
		public Coord coord;
		public Coord fromCoord;
		public NodeCoord(Coord coord, Coord fromCoord) {
			super();
			this.coord = coord;
			this.fromCoord = fromCoord;
		}
		
		@Override
		public int hashCode() {
			return (int) (coord.hashCode()*(fromCoord==null?1:fromCoord.hashCode()));
		}
		
		@Override
		public String toString() {
			return coord+" , "+fromCoord;
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof NodeCoord)
				if(coord.equals(((NodeCoord)o).coord) && (fromCoord==((NodeCoord)o).fromCoord || fromCoord.equals(((NodeCoord)o).fromCoord)))
					return true;
			return false;
		}

	}
	/**
	 * Class to represent a coordinate
	 * @author sergioo
	 *
	 */
	public static class Coord {
		public double x;
		public double y;

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
		
		double getDistance(Coord coord) {
			return Math.sqrt((x-coord.x)*(x-coord.x) + (y-coord.y)*(y-coord.y)); 
		}
	}
	/**
	 * Class to represent geometry and graphic object of each road
	 * @author sergioo
	 *
	 */
	public static class RoadInfo {
		public NodeCoord cF;
		public NodeCoord cT;
		IGeometry p;
		private int originId;
		private int networkId;
		public List<RelAccess> locs = new ArrayList<RelAccess>();
		
		public RoadInfo(NodeCoord cF, NodeCoord cT, IGeometry p, int originId, int networkId) {
			super();
			this.cF = cF;
			this.cT = cT;
			this.p = p;
			this.originId = originId;
			this.networkId = networkId;
		}
	}
	
	public static class RelAccess {
		public Integer cF;
		public IGeometry p;
		public Double size;
		public RelAccess(Integer cF, IGeometry p, Double size) {
			super();
			this.cF = cF;
			this.p = p;
			this.size = size;
		}
	}
	/**
	 * Called when the button is clicked.
	 * 
	 * @exception java.io.IOException if there are interop problems.
	 * @exception com.esri.arcgis.interop.AutomationException if the component throws an ArcObjects exception.
	 */
	@Override
	public void onClick() throws IOException, AutomationException {
		if(isEnabled) {
			final Map map = (Map) ((IMxDocument) app.getDocument()).getActiveView();
			boolean moreThanOne = false;
			IPoint point = null;
			Integer pointInt = null;
			ICursor[] cursorS = new ICursor[1];
			ISelectionSet sSet = PrepareButton.locationsLayer.getSelectionSet();
			sSet.search(null, true, cursorS);
			IFeatureCursor fCursor = new FeatureCursor(cursorS[0]);
			IFeature feature = fCursor.nextFeature();
			while(feature!=null) {
				if(feature.getShape() instanceof Point && point==null) {
					point = (IPoint) feature.getShapeCopy();
					pointInt = feature.getOID();
				}
				else if(feature.getShape() instanceof Point)
					moreThanOne = true;
				feature = fCursor.nextFeature();
			}
			if (!moreThanOne && point!=null) {
				if(map.getActiveGraphicsLayer()!=null && map.getActiveGraphicsLayer() instanceof FeatureLayer && ((FeatureLayer)map.getActiveGraphicsLayer()).getDataSourceType().startsWith(PrepareButton.DATA_TYPE) && map.getActiveGraphicsLayer()!=PrepareButton.roadsCostLayer)
					JOptionPane.showMessageDialog(null, "The selected layer can not be modified anymore. The process will be applied to the scenario: "+PrepareButton.scenarioName);
				final JFrame windowP = new JFrame();
				windowP.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				windowP.setAlwaysOnTop(true);
				windowP.setLocationRelativeTo(null);
				windowP.setUndecorated(true);
				windowP.setLayout(new GridLayout(2, 1));
				JLabel lProgress = new JLabel();
				lProgress.setHorizontalAlignment(SwingConstants.LEFT);
				JProgressBar progressBar = new JProgressBar(0, 100);
				progressBar.setStringPainted(true);
				windowP.add(progressBar);
				windowP.add(lProgress);
				windowP.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				windowP.pack();
				windowP.setVisible(true);
				progressBar.setValue(0);
				lProgress.setText("Preparing subgraph");
				boolean added = false;
				for(int i=0; i<map.getLayerCount(); i++)
					if(map.getLayer(i).getName().equals(PrepareButton.roadsCostLayer.getName()))
						added = true;
				if(!added) {
					map.addLayer(PrepareButton.extraLayer);
					map.addLayer(PrepareButton.roadsCostLayer);
					map.addLayer(PrepareButton.locsCostLayer);
				}
				try {
					PrepareButton.roadsCostLayer.setVisible(true);
					PrepareButton.locsCostLayer.setVisible(true);
					PrepareButton.extraLayer.setVisible(true);
					PrepareButton.roadsCostLayer.setRendererByRef(getRoadsRenderer(0, ParametersButton.max));
					PrepareButton.locsCostLayer.setRendererByRef(getLocationsRenderer(0, ParametersButton.max));
					PrepareButton.extraLayer.setRendererByRef(getExtraRenderer());
					//Run shortest path algorithm 1 to n
					progressBar.setValue(2);
					NodeCoord c = new NodeCoord(new Coord(((IPoint)point).getX(), ((IPoint)point).getY()), null);
					RoadInfo road = PrepareButton.nearestRoads.get(pointInt);
					DefaultDirectedWeightedGraph<NodeCoord, DefaultWeightedEdge> subGraph = new DefaultDirectedWeightedGraph<NodeCoord, DefaultWeightedEdge>(DefaultWeightedEdge.class);
					double maxDis = 2*ParametersButton.max*ParametersButton.speed;
					for(DefaultWeightedEdge edge:PrepareButton.graph.edgeSet()) {
						NodeCoord cF = PrepareButton.graph.getEdgeSource(edge);
						NodeCoord cT = PrepareButton.graph.getEdgeTarget(edge);
						if(cF.coord.getDistance(c.coord)<maxDis || cT.coord.getDistance(c.coord)<maxDis) {
							subGraph.addVertex(cF);
							subGraph.addVertex(cT);
							DefaultWeightedEdge edgeN = subGraph.addEdge(cF, cT);
							subGraph.setEdgeWeight(edgeN, PrepareButton.graph.getEdgeWeight(edge));
						}
					}
					progressBar.setValue(5);
					/*progressBar.setToolTipText("Rotation links");
					lProgress.setText("Rotation links");
					for(CalculateButton.NodeCoord nCoord:subGraph.vertexSet())
						for(CalculateButton.NodeCoord nCoordO:subGraph.vertexSet())
							if(nCoordO.coord.equals(nCoord.coord) && !nCoordO.fromCoord.equals(nCoord.fromCoord)) {
								DefaultWeightedEdge nEdge = subGraph.addEdge(nCoord, nCoordO);
								double x = nCoord.coord.x, xa = nCoord.fromCoord.x, xb = nCoordO.coord.x;
								double y = nCoord.coord.y, ya = nCoord.fromCoord.y, yb = nCoordO.coord.y;
								double angle = Math.acos(((x-xa)*(xb-x)+(y-ya)*(yb-y))/(Math.hypot(x-xa, y-ya)*Math.hypot(xb-x, yb-y)));
								subGraph.setEdgeWeight(nEdge, ParametersButton.rotationTime*angle/Math.PI);
							}*/
					subGraph.addVertex(c);
					DefaultWeightedEdge edgeF = subGraph.addEdge(c, road.cF);
					DefaultWeightedEdge edgeT = subGraph.addEdge(c, road.cT);
					double costRoad = subGraph.getEdgeWeight(subGraph.getEdge(road.cF, road.cT));
					subGraph.setEdgeWeight(edgeF, costRoad*c.coord.getDistance(road.cF.coord)/road.cT.coord.getDistance(road.cF.coord));
					subGraph.setEdgeWeight(edgeT, costRoad*c.coord.getDistance(road.cT.coord)/road.cF.coord.getDistance(road.cT.coord));
					progressBar.setValue(20);
					progressBar.setToolTipText("Shortest path algorithm");
					lProgress.setText("One-to-N shortest path");
					BellmanFordShortestPath<NodeCoord, DefaultWeightedEdge> algo = new BellmanFordShortestPath<NodeCoord, DefaultWeightedEdge>(subGraph, c);
					algo.getCost(road.cF);
					progressBar.setValue(50);
					//Delete all rows of the roads layer
					PrepareButton.roadsCostLayer.deleteSearchedRows(null);
					PrepareButton.locsCostLayer.deleteSearchedRows(null);
					PrepareButton.extraLayer.deleteSearchedRows(null);
					//Obtain the current table (feature class), find the cost field and create a feature buffer
					IFeatureClass featureClass = PrepareButton.roadsCostLayer.getFeatureClass();
					IFeatureClass featureClassL = PrepareButton.locsCostLayer.getFeatureClass();
					int pathCostIndex = featureClass.findField("PathCost"); 
					int costIndex = featureClass.findField("Cost"); 
					int originIndex = featureClass.findField("OriginId"); 
					int networkIndex = featureClass.findField("NetworkId"); 
					int pathCostIndexL = featureClassL.findField("PathCost"); 
					int pathCostCatIndexL = featureClassL.findField("PathCostCat"); 
					int originIndexL = featureClassL.findField("OriginId"); 
					int sizeIndexL = featureClassL.findField("Size"); 
					IFeatureBuffer featureBuffer = featureClass.createFeatureBuffer();
					IFeatureBuffer featureBufferL = featureClassL.createFeatureBuffer();
					//For each road insert a row in the road feature layer wit an updated cost
					IFeatureCursor cursor = featureClass.IFeatureClass_insert(true);
					IFeatureCursor cursorL = featureClassL.IFeatureClass_insert(true);
					int num = 0;
					Multipoint array = new Multipoint();
					Multipoint arrayL = new Multipoint();
					int totalNum = 0;
					double totalDistance = 0;
					double totalTime = 0;
					double totalNumL = 0;
					double totalAcc = 0;
					double totalAccW = 0;
					lProgress.setText("Updating results");
					for(RoadInfo roadInfo:roadsInfo.values()) {
						NodeCoord cF = roadInfo.cF;
						NodeCoord cT = roadInfo.cT;
						double cost = 999999;
						double pathCost = 999999;
						double fCost = 999999;
						double tCost = 999999;
						if(subGraph.containsEdge(cF, cT)) {
							fCost = cF.equals(c)?0:algo.getCost(cF);
							tCost = cT.equals(c)?0:algo.getCost(cT);
							pathCost = (fCost+tCost)/2;
							cost = subGraph.getEdgeWeight(PrepareButton.graph.getEdge(cF, cT));
							if(pathCost<=ParametersButton.max) {
								totalNum++;
								Polyline p = (Polyline) roadInfo.p;
								totalDistance += p.getLength();
								totalTime += cost;
								array.addPoint(p.getFromPoint(), null, null);
								array.addPoint(p.getToPoint(), null, null);
							}
						}
						featureBuffer.setShapeByRef(roadInfo.p);	
						featureBuffer.setValue(pathCostIndex, pathCost>=999999?999999:pathCost);
						featureBuffer.setValue(costIndex, cost>=999999?999999:cost);
						featureBuffer.setValue(originIndex, roadInfo.originId);
						featureBuffer.setValue(networkIndex, roadInfo.networkId);
						cursor.insertFeature(featureBuffer);
						for(int i=0; i<roadInfo.locs.size(); i++) {
							IPoint pointL = (IPoint) roadInfo.locs.get(i).p;
							Coord cP = new Coord(pointL.getX(), pointL.getY());
							featureBufferL.setShapeByRef(pointL);
							double pathCostL = Math.min(fCost+cost*cP.getDistance(cF.coord)/cT.coord.getDistance(cF.coord), tCost+cost*cP.getDistance(cT.coord)/cT.coord.getDistance(cF.coord));
							featureBufferL.setValue(pathCostIndexL, pathCostL>=999999?999999:pathCostL);
							int cat = (int)(pathCostL*PrepareButton.SIZE/ParametersButton.max);
							featureBufferL.setValue(pathCostCatIndexL, cat<PrepareButton.SIZE?cat:-1);
							featureBufferL.setValue(originIndexL, roadInfo.locs.get(i).cF);
							double size = roadInfo.locs.get(i).size;
							featureBufferL.setValue(sizeIndexL, size);
							if(pathCostL<=ParametersButton.max) {
								totalNumL++;
								totalAcc+=size;
								totalAccW+=size*Math.exp(ParametersButton.lambda*pathCostL);
								arrayL.addPoint(pointL, null, null);
							}
							cursorL.insertFeature(featureBufferL);
						}
						progressBar.setValue(50+(40*++num/roadsInfo.size()));
					}
					cursorL.flush();
					cursor.flush();
					map.refresh();
					//Fill extra layer
					IFeatureClass featureClassE = PrepareButton.extraLayer.getFeatureClass();
					int indexPoly = featureClassE.findField("Number");
					double area = 0;
					if(!array.isEmpty()) {
						IGeometry convex = array.convexHull();
						if(convex instanceof IPolygon) {
							cursor = featureClassE.IFeatureClass_insert(true);
							featureBuffer = featureClassE.createFeatureBuffer();
							featureBuffer.setShapeByRef(convex);
							featureBuffer.setValue(indexPoly, 1);
							cursor.insertFeature(featureBuffer);
							area = ((IArea)convex).getArea();
						}
					}
					/*double areaL = 0;
					if(!arrayL.isEmpty()) {
						IGeometry convexL = arrayL.convexHull();
						featureBuffer = featureClassE.createFeatureBuffer();
						featureBuffer.setShapeByRef(convexL);
						featureBuffer.setValue(indexPoly, 2);
						cursor.insertFeature(featureBuffer);
						areaL = convexL instanceof IArea?((IArea)convexL).getArea():0;
					}*/
					Polygon circle = new Polygon();
					for(int i=0;i<SIZE_CIRCLE+1;i++) {
						Point pointC = new Point();
						pointC.putCoords(c.coord.x+ParametersButton.max*ParametersButton.speed*Math.cos(i*2*Math.PI/SIZE_CIRCLE), c.coord.y+ParametersButton.max*ParametersButton.speed*Math.sin(i*2*Math.PI/SIZE_CIRCLE));
						circle.addPoint(pointC, null, null);
					}
					featureBuffer = featureClassE.createFeatureBuffer();
					featureBuffer.setShapeByRef(circle);
					featureBuffer.setValue(indexPoly, 3);
					cursor.insertFeature(featureBuffer);
					cursor.flush();
					final JFrame windowL = new JFrame("Results");
					windowL.setIconImage(PrepareButton.icon);
					windowL.setAlwaysOnTop(true);
					windowL.setLocationRelativeTo(null);
					windowL.setLayout(new BorderLayout());
					JPanel panelAux = new JPanel();
					panelAux.setLayout(new BorderLayout());
					JPanel panelLinks  = new JPanel();
					panelLinks.setLayout(new GridLayout(6, 2));
					final Double[] values = new Double[] {
							(double)totalNum,
							(int)(totalDistance/10.0)/100.0,
							(int)(totalTime/0.6)/100.0,
							(int)(area/10000.0)/100.0,
							(int)(totalTime*ParametersButton.speed*100/totalDistance)/100.0,
							(int)(area*100/(Math.PI*Math.pow(ParametersButton.max*ParametersButton.speed,2)))/100.0,
							totalNumL,
							(double)((int)totalAcc),
							(double)((int)totalAccW)/*,
							(int)(areaL*100/(Math.PI*Math.pow(ParametersButton.max*ParametersButton.speed,2)))/100.0*/
					};
					panelLinks.setBorder(new TitledBorder("Links statistics:"));
					panelLinks.add(new JLabel("Number of accessible links: "));
					panelLinks.add(new JLabel(values[0]+""));
					panelLinks.add(new JLabel("Total accessible distance: "));
					panelLinks.add(new JLabel(values[1]+" kilometers"));
					panelLinks.add(new JLabel("Total perceived time: "));
					panelLinks.add(new JLabel(values[2]+" hours"));
					panelLinks.add(new JLabel("Accesible area: "));
					panelLinks.add(new JLabel(values[3]+" squared kilometers"));
					panelLinks.add(new JLabel("Perceived distance ratio: "));
					panelLinks.add(new JLabel(values[4]+""));
					panelLinks.add(new JLabel("Links walkshed ratio: "));
					panelLinks.add(new JLabel(values[5]+""));
					panelAux.add(panelLinks, BorderLayout.NORTH);
					JPanel panelLocs  = new JPanel();
					panelLocs.setLayout(new GridLayout(3, 2));
					panelLocs.setBorder(new TitledBorder("Entries statistics:"));
					panelLocs.add(new JLabel("Number of accessible entrances: "));
					panelLocs.add(new JLabel(values[6]+""));
					panelLocs.add(new JLabel("Total accessible size: "));
					panelLocs.add(new JLabel(values[7]+""));
					panelLocs.add(new JLabel("Total accessible weighted size: "));
					panelLocs.add(new JLabel(values[8]+""));
					panelAux.add(panelLocs, BorderLayout.SOUTH);
					JPanel panelAux2 = new JPanel();
					panelAux2.setLayout(new GridLayout(1, 2));
					JButton jButton = new JButton("Save");
					jButton.addActionListener(new ActionListener() {	
						public void actionPerformed(ActionEvent e) {
							JFileChooser chooser = new JFileChooser();
							int a = chooser.showSaveDialog(windowL);
							if(a==JFileChooser.APPROVE_OPTION) {
								try {
									PrintWriter printWriter = new PrintWriter(chooser.getSelectedFile());
									printWriter.println("Number of accessible links,"+values[0]);
									printWriter.println("Total accessible distance (km),"+values[1]);
									printWriter.println("Total perceived time (h),"+values[2]);
									printWriter.println("Accesible area (sqr-km),"+values[3]);
									printWriter.println("Perceived distance ratio,"+values[4]);
									printWriter.println("Links walkshed ratio,"+values[5]);
									printWriter.println("Number of accessible entrances,"+values[6]);
									printWriter.println("Total accessible size,"+values[7]);
									printWriter.println("Total accessible weighted size,"+values[8]);
									printWriter.close();
								} catch (FileNotFoundException e1) {
									JOptionPane.showMessageDialog(null, e1.getMessage());
								}
							}
						}
					});
					panelAux2.add(jButton);
					jButton = new JButton("Continue");
					jButton.addActionListener(new ActionListener() {	
						public void actionPerformed(ActionEvent e) {
							windowL.setVisible(false);
						}
					});
					panelAux2.add(jButton);
					windowL.add(panelAux, BorderLayout.NORTH);
					windowL.add(panelAux2, BorderLayout.SOUTH);
					windowL.pack();
					progressBar.setValue(100);
					windowP.setVisible(false);
					windowL.setVisible(true);
				} catch (AutomationException e1) {
					/*for(StackTraceElement trace:e1.getStackTrace())
						JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());*/
					JOptionPane.showMessageDialog(null, e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(null, e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
				} catch (NullPointerException e1) {
					/*for(StackTraceElement trace:e1.getStackTrace())
						JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());*/
					JOptionPane.showMessageDialog(null, "null: "+e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
				} catch (Exception e1) {
					/*for(StackTraceElement trace:e1.getStackTrace())
						JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());*/
					JOptionPane.showMessageDialog(null, "other: "+e1.getClass().toString()+" "+e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
				}
			}
			else {
				map.clearSelection();
				JOptionPane.showMessageDialog(null, "Select one and only one point from the locations layer: "+PrepareButton.locationsLayer.getName());
			}
		}
		else
			JOptionPane.showMessageDialog(null, "Parameters have not been defined");
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
	private ClassBreaksRenderer getRoadsRenderer(double min, double max) throws UnknownHostException, IOException {
		ClassBreaksRenderer costRenderer = new ClassBreaksRenderer(); 
		costRenderer.setField("PathCost");
		costRenderer.setBreakCount(PrepareButton.SIZE);
		costRenderer.setExclusionSymbol(exclusionSymbol);
		costRenderer.setMinimumBreak(min-2);
		costRenderer.setShowExclusionClass(true);
		double i=min;
		for(int n=0; n<PrepareButton.SIZE; n++) {
			costRenderer.setBreak(n, i+=(max-min)/PrepareButton.SIZE);
			costRenderer.setSymbol(n, (ISymbol)edgeSymbols[n]);
		}
		return costRenderer;
	}
	
	private BiUniqueValueRenderer getLocationsRenderer(double min, double max) throws UnknownHostException, IOException {
		BiUniqueValueRenderer biUniqueValueRenderer = new BiUniqueValueRenderer();
		UniqueValueRenderer costRenderer = new UniqueValueRenderer(); 
		costRenderer.setFieldCount(1);
		costRenderer.setField(0,"PathCostCat");
		costRenderer.setDefaultSymbol(exclusionSymbolL);
		costRenderer.setUseDefaultSymbol(true);
		for(int n=0; n<PrepareButton.SIZE; n++) {
			costRenderer.addValue(n+"", "PathCostCat", (ISymbol)locationSymbolsC[n]);
			costRenderer.setLabel(n+"", n+"");
			costRenderer.setSymbol(n+"", (ISymbol)locationSymbolsC[n]);
		}
		ClassBreaksRenderer sizeRenderer = new ClassBreaksRenderer(); 
		sizeRenderer.setField("Size");
		sizeRenderer.setBreakCount(PrepareButton.SIZE);
		sizeRenderer.setExclusionSymbol(exclusionSymbol);
		sizeRenderer.setMinimumBreak(min);
		sizeRenderer.setShowExclusionClass(true);
		double i=0;
		for(int n=0; n<PrepareButton.SIZE; n++) {
			sizeRenderer.setLowBreak(n, i);
			sizeRenderer.setBreak(n, i+=(int)((PrepareButton.maxSize-0)/PrepareButton.SIZE));
			sizeRenderer.setSymbol(n, (ISymbol)locationSymbolsS[n]);
		}
		biUniqueValueRenderer.setMainRendererByRef(costRenderer);
		biUniqueValueRenderer.setVariationRendererByRef(sizeRenderer);
		return biUniqueValueRenderer;
	}
	
	/*private BivariateRenderer getLocationsRenderer(double min, double max) throws UnknownHostException, IOException {
		BivariateRenderer costRenderer = new BivariateRenderer();
		costRenderer.setSymbols(locationSymbols);
		costRenderer.setExclusionSymbol(exclusionSymbol);
		costRenderer.setField("PathCost");
		costRenderer.setMin(min);
		costRenderer.setMax(max);
		costRenderer.setNum(PrepareButton.SIZE);
		costRenderer.setField2("Size");
		costRenderer.setMin2(0);
		costRenderer.setMax2(PrepareButton.maxSize);
		costRenderer.setNum2(PrepareButton.SIZE);
		return costRenderer;
	}*/
	
	private ClassBreaksRenderer getExtraRenderer() throws UnknownHostException, IOException {
		ClassBreaksRenderer costRenderer = new ClassBreaksRenderer(); 
		costRenderer.setField("Number");
		costRenderer.setBreakCount(3);
		costRenderer.setBreak(0, 1);
		costRenderer.setSymbol(0, polygonSymbol1);
		costRenderer.setBreak(1, 2);
		costRenderer.setSymbol(1, polygonSymbol2);
		costRenderer.setBreak(2, 3);
		costRenderer.setSymbol(2, circleSymbol);
		return costRenderer;
	}

}
