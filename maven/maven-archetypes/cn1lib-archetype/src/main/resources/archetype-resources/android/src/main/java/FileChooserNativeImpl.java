#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

public class FileChooserNativeImpl {
    public boolean isSupported() {
        return true;
    }
    
    public boolean showNativeChooser(String accept, boolean multi) {
        return false;
    }

}
