namespace com.codename1.impl {

    public class SilverlightPeerFactoryImpl : com.codename1.impl.SilverlightPeerFactory {

        public override com.codename1.ui.PeerComponent wrap(object o) {
            return new com.codename1.impl.SilverlightPeer((Windows.UI.Xaml.FrameworkElement)o);
        }
        public override object unwrap(com.codename1.ui.PeerComponent o) {

            return ((com.codename1.impl.SilverlightPeer)o).element;
        }
    }
}