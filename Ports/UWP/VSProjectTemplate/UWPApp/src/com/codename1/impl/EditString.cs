namespace com.codename1.impl
{
    class EditString : java.lang.Object, java.lang.Runnable
    {
        private ui.Component n1;
        private int n2;
        private int n3;
        private string n4;
        private int n5;

        public EditString(ui.Component n1, int n2, int n3, string n4, int n5)
        {
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.n4 = n4;
            this.n5 = n5;
        }

        public virtual void run()
        {
            ui.Display d = (ui.Display)com.codename1.ui.Display.getInstance();
            d.editString(n1, n2, n3, n4, n5);
            ((ui.Form)n1.getComponentForm()).repaint();
        }
    }
}
