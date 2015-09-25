import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.esri.arcgis.addins.desktop.Button;
import com.esri.arcgis.framework.IApplication;
import com.esri.arcgis.framework.IDockableWindow;
import com.esri.arcgis.framework.IDockableWindowManager;
import com.esri.arcgis.framework.IDockableWindowManagerProxy;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.system.UID;


public class ButtonSergio extends Button {

	private IDockableWindow docWin;
	private IApplication app;
	
	@Override
	public void init(IApplication app){
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
		if(docWin==null) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			} catch (InstantiationException e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			} catch (IllegalAccessException e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			} catch (UnsupportedLookAndFeelException e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			}
			try{
		        IDockableWindowManager dwm = new IDockableWindowManagerProxy(app);
	
		        //Create a UID object.
		        UID uid = new UID();
		        //Obtain the Dockable Window id from your Dockable Window Add-In Editor and set the UID with this ID
		        uid.setValue("dockablewindowSergio");
	
		        //Get the dockable window object based on its ID.
		        docWin = dwm.getDockableWindow(uid);
		    }
		    catch (Exception e){
		        e.printStackTrace();
		    }
		}
		docWin.show(!docWin.isVisible());
	}

}
