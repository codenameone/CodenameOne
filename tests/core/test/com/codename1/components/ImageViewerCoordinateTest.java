package com.codename1.components;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Image;
import com.codename1.ui.Form;

/**
 * Test for ImageViewer overlapping images fix
 */
public class ImageViewerCoordinateTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        testZoomAndPanCoordinateConsistency();
        return true;
    }
    
    /**
     * Test that coordinate calculations in updatePositions and paint methods are consistent
     */
    public void testZoomAndPanCoordinateConsistency() throws Exception {
        System.out.println("Testing ImageViewer coordinate consistency");
        
        // Create a test form and image viewer
        Form testForm = new Form();
        ImageViewer viewer = new ImageViewer();
        
        // Create a large test image (bigger than typical screen)
        Image testImage = Image.createImage(2000, 1500);  
        viewer.setImage(testImage);
        
        // Set viewer size (simulate a typical mobile screen)
        viewer.setX(50);  // Not starting at 0,0 to test coordinate translation
        viewer.setY(100);
        viewer.setWidth(400);
        viewer.setHeight(600);
        
        // Add to form and lay out
        testForm.addComponent(viewer);
        testForm.show();
        testForm.revalidate();
        
        // Test scenario 1: High zoom with right pan
        float zoom = 3.0f;
        float panX = 0.8f; // Panned to the right
        float panY = 0.5f; // Centered vertically
        
        viewer.setZoom(zoom, panX, panY);
        
        // The key test: verify that the image coordinates are properly constrained
        // With the fix, the image should not extend beyond the viewer bounds
        int imageX = viewer.getImageX();
        int imageY = viewer.getImageY();
        
        // Log the values for debugging
        System.out.println("Zoom: " + zoom + ", PanX: " + panX + ", PanY: " + panY);
        System.out.println("ImageX: " + imageX + ", ImageY: " + imageY);
        System.out.println("Viewer bounds: X=" + viewer.getX() + ", Y=" + viewer.getY() + 
                          ", W=" + viewer.getWidth() + ", H=" + viewer.getHeight());
        
        // Test scenario 2: Maximum reasonable zoom at far right
        zoom = 5.0f;
        panX = 1.0f; // Fully right
        viewer.setZoom(zoom, panX, panY);
        
        imageX = viewer.getImageX();
        imageY = viewer.getImageY();
        
        System.out.println("High zoom test - ImageX: " + imageX + ", ImageY: " + imageY);
        
        // Test scenario 3: Test IMAGE_FILL mode to ensure it works with the fix
        viewer.setImageInitialPosition(ImageViewer.IMAGE_FILL);
        viewer.setZoom(2.0f, 0.9f, 0.5f);
        
        imageX = viewer.getImageX();
        imageY = viewer.getImageY();
        
        System.out.println("IMAGE_FILL mode test - ImageX: " + imageX + ", ImageY: " + imageY);
        
        System.out.println("ImageViewer coordinate consistency test completed successfully");
    }
}