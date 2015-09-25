import java.awt.Cursor;
import java.awt.GridLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.esri.arcgis.addins.desktop.Button;
import com.esri.arcgis.arcmapui.IMxDocument;
import com.esri.arcgis.carto.FeatureLayer;
import com.esri.arcgis.carto.Map;
import com.esri.arcgis.framework.IApplication;
import com.esri.arcgis.geodatabase.FeatureCursor;
import com.esri.arcgis.geodatabase.ICursor;
import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.ISelectionSet;
import com.esri.arcgis.geometry.IArea;
import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.IPoint;
import com.esri.arcgis.geometry.Multipoint;
import com.esri.arcgis.geometry.Point;
import com.esri.arcgis.geometry.Polyline;
import com.esri.arcgis.interop.AutomationException;


public class CalculateBulkButton extends Button {

	private IApplication app;
	public static boolean isEnabled = false;
	
	@Override
	public void init(IApplication app) throws AutomationException, IOException {
	    this.app = app;
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
			List<IPoint> points = new ArrayList<IPoint>();
			List<Integer> pointInts = new ArrayList<Integer>();
			ICursor[] cursorS = new ICursor[1];
			ISelectionSet sSet = PrepareButton.locationsLayer.getSelectionSet();
			sSet.search(null, true, cursorS);
			IFeatureCursor fCursor = new FeatureCursor(cursorS[0]);
			IFeature feature = fCursor.nextFeature();
			while(feature!=null) {
				if(feature.getShape() instanceof Point) {
					points.add((IPoint) feature.getShapeCopy());
					pointInts.add(feature.getOID());
				}
				feature = fCursor.nextFeature();
			}
			if (points.size()>0) {
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
				lProgress.setText("Preparing subgraph");
				JFileChooser chooser = new JFileChooser();
				int a = chooser.showSaveDialog(null);
				PrintWriter printWriter = new PrintWriter(chooser.getSelectedFile());
				printWriter.println("Id,Number of accessible links,Total accessible distance,Total perceived time,Accesible area,Perceived distance ratio,Links walkshed ratio,Number of accessible entrances,Total accessible size,Total accessible weighted size");
				windowP.setVisible(true);
				if(a==JFileChooser.APPROVE_OPTION) {
					for(int l=0; l<points.size(); l++)
						try {
							//Run shortest path algorithm 1 to n
							progressBar.setValue(0);
							IPoint point = points.get(l);
							Integer pointInt = pointInts.get(l);
							progressBar.setValue(2);
							CalculateButton.NodeCoord c = new CalculateButton.NodeCoord(new CalculateButton.Coord(((IPoint)point).getX(), ((IPoint)point).getY()), null);
							CalculateButton.RoadInfo road = PrepareButton.nearestRoads.get(pointInt);
							DefaultDirectedWeightedGraph<CalculateButton.NodeCoord, DefaultWeightedEdge> subGraph = new DefaultDirectedWeightedGraph<CalculateButton.NodeCoord, DefaultWeightedEdge>(DefaultWeightedEdge.class);
							double maxDis = 2*ParametersButton.max*ParametersButton.speed;
							for(DefaultWeightedEdge edge:PrepareButton.graph.edgeSet()) {
								CalculateButton.NodeCoord cF = PrepareButton.graph.getEdgeSource(edge);
								CalculateButton.NodeCoord cT = PrepareButton.graph.getEdgeTarget(edge);
								if(cF.coord.getDistance(c.coord)<maxDis || cT.coord.getDistance(c.coord)<maxDis) {
									subGraph.addVertex(cF);
									subGraph.addVertex(cT);
									DefaultWeightedEdge edgeN = subGraph.addEdge(cF, cT);
									subGraph.setEdgeWeight(edgeN, PrepareButton.graph.getEdgeWeight(edge));
								}
							}
							subGraph.addVertex(c);
							DefaultWeightedEdge edgeF = subGraph.addEdge(c, road.cF);
							DefaultWeightedEdge edgeT = subGraph.addEdge(c, road.cT);
							double costRoad = subGraph.getEdgeWeight(subGraph.getEdge(road.cF, road.cT));
							subGraph.setEdgeWeight(edgeF, costRoad*c.coord.getDistance(road.cF.coord)/road.cT.coord.getDistance(road.cF.coord));
							subGraph.setEdgeWeight(edgeT, costRoad*c.coord.getDistance(road.cT.coord)/road.cF.coord.getDistance(road.cT.coord));
							progressBar.setValue(5);
							progressBar.setToolTipText("Rotation links");
							lProgress.setText("Rotation links");
							for(CalculateButton.NodeCoord nCoord:subGraph.vertexSet())
								for(CalculateButton.NodeCoord nCoordO:subGraph.vertexSet())
									if(nCoordO.coord.equals(nCoord.coord) && !nCoordO.fromCoord.equals(nCoord.fromCoord)) {
										DefaultWeightedEdge nEdge = subGraph.addEdge(nCoord, nCoordO);
										double x = nCoord.coord.x, xa = nCoord.fromCoord.x, xb = nCoordO.coord.x;
										double y = nCoord.coord.y, ya = nCoord.fromCoord.y, yb = nCoordO.coord.y;
										double angle = Math.acos(((x-xa)*(xb-x)+(y-ya)*(yb-y))/(Math.hypot(x-xa, y-ya)*Math.hypot(xb-x, yb-y)));
										PrepareButton.graph.setEdgeWeight(nEdge, ParametersButton.rotationTime*angle/Math.PI);
									}
							progressBar.setValue(30);
							progressBar.setToolTipText("Shortest path algorithm");
							lProgress.setText("One-to-N shortest path");
							BellmanFordShortestPath<CalculateButton.NodeCoord, DefaultWeightedEdge> algo = new BellmanFordShortestPath<CalculateButton.NodeCoord, DefaultWeightedEdge>(subGraph, c);
							algo.getCost(road.cF);
							progressBar.setValue(80);
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
							for(CalculateButton.RoadInfo roadInfo:CalculateButton.roadsInfo.values()) {
								CalculateButton.NodeCoord cF = roadInfo.cF;
								CalculateButton.NodeCoord cT = roadInfo.cT;
								double cost = 999999;
								double pathCost = 999999;
								if(subGraph.containsEdge(cF, cT)) {
									double fCost = cF.equals(c)?0:algo.getCost(cF);
									double tCost = cT.equals(c)?0:algo.getCost(cT);
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
									for(int i=0; i<roadInfo.locs.size(); i++) {
										IPoint pointL = (IPoint) roadInfo.locs.get(i).p;
										CalculateButton.Coord cP = new CalculateButton.Coord(pointL.getX(), pointL.getY());
										double pathCostL = pathCost+cost*Math.min(cP.getDistance(cF.coord), cP.getDistance(cT.coord))/cT.coord.getDistance(cF.coord);
										double size = roadInfo.locs.get(i).size;
										if(pathCostL<=ParametersButton.max) {
											totalNumL++;
											totalAcc+=size;
											totalAccW+=size*Math.exp(ParametersButton.lambda*pathCostL);
											arrayL.addPoint(pointL, null, null);
										}
									}
								}
								progressBar.setValue(80+(15*++num/CalculateButton.roadsInfo.size()));
							}
							map.refresh();
							//Fill extra layer
							double area = 0;
							if(!array.isEmpty()) {
								IGeometry convex = array.convexHull();
								area= convex instanceof IArea?((IArea)convex).getArea():0;
							}
							/*double areaL = 0;
							if(!arrayL.isEmpty()) {
								IGeometry convexL = arrayL.convexHull();
								areaL= convexL instanceof IArea?((IArea)convexL).getArea():0;
							}*/
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
							progressBar.setValue(100);
							printWriter.print(pointInt);
							for(double value:values)
								printWriter.print(","+value);
							printWriter.println();
						} catch (FileNotFoundException e1) {
							JOptionPane.showMessageDialog(null, e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
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
				windowP.setVisible(false);
				printWriter.close();
			}
			else {
				map.clearSelection();
				JOptionPane.showMessageDialog(null, "Select at least one point of the locations layer: "+PrepareButton.locationsLayer.getName());
			}
		}
		else
			JOptionPane.showMessageDialog(null, "Parameters have not been defined");
	}

}
