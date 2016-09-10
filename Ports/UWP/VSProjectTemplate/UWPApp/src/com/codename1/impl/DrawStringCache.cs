namespace com.codename1.impl
{

    public class DrawStringCache
    {
        public string str;
        public int color;
        public NativeFont font;
        public long lastAccess;

        public override bool Equals(object obj)
        {
            DrawStringCache d = (DrawStringCache)obj;
            return str.Equals(d.str) && color == d.color && font == d.font;
        }

        public override int GetHashCode()
        {
            return str.GetHashCode() + color;
        }
    }
}
