namespace com.codename1.impl
{
    class Tchange : object, java.lang.Runnable
    {
        public ui.TextArea currentlyEditing;
        public string text;
        public virtual void run()
        {
            if (currentlyEditing != null)
            {
                currentlyEditing.setText(text);
            }
        }
    }
}
