using System;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;

namespace java.nio
{
    internal sealed class CharSequenceAdapter : CharBuffer
    {

        static CharSequenceAdapter copy(CharSequenceAdapter other)
        {
            CharSequenceAdapter buf = new CharSequenceAdapter(other.sequence);
            buf._flimit = other._flimit;
            buf._fposition = other._fposition;
            buf._fmark = other._fmark;
            return buf;
        }

        internal readonly java.lang.CharSequence sequence;

        internal CharSequenceAdapter(java.lang.CharSequence chseq)
        {
            base.@this(chseq.length());
            sequence = chseq;
        }

        public override global::System.Object asReadOnlyBuffer()
        {
            return duplicate();
        }

        public override global::System.Object compact()
        {
            throw new global::org.xmlvm._nExceptionAdapter(new ReadOnlyBufferException());
        }

        public override global::System.Object duplicate()
        {
            return copy(this);
        }

        public override char get()
        {
            if (_fposition == _flimit)
            {
                throw new global::org.xmlvm._nExceptionAdapter(new BufferUnderflowException());
            }
            return sequence.charAt(_fposition++);
        }

        public override char get(int index)
        {
            if (index < 0 || index >= _flimit)
            {
                throw new System.IndexOutOfRangeException();
            }
            return sequence.charAt(index);
        }

        public override global::System.Object get(global::org.xmlvm._nArrayAdapter<char> dest, int off, int len)
        {
            int length = dest.Length;
            if ((off < 0) || (len < 0) || (long)off + (long)len > length)
            {
                throw new System.IndexOutOfRangeException();
            }
            if (len > remaining())
            {
                throw new global::org.xmlvm._nExceptionAdapter(new BufferUnderflowException());
            }
            int newPosition = _fposition + len;
            ((global::java.lang.String)sequence.toString()).getChars(_fposition, newPosition, dest, off);
            _fposition = newPosition;
            return this;
        }

        public override bool isDirect()
        {
            return false;
        }

        public override bool isReadOnly()
        {
            return true;
        }

        public override global::System.Object order()
        {
            return (ByteOrder)ByteOrder.nativeOrder();
        }

        public override global::System.Object protectedArray()
        {
            throw new System.NotSupportedException();
        }

        public override int protectedArrayOffset()
        {
            throw new System.NotSupportedException();
        }

        public override bool protectedHasArray()
        {
            return false;
        }

        public override global::System.Object put(char c)
        {
            throw new global::org.xmlvm._nExceptionAdapter(new ReadOnlyBufferException());
        }

        public override global::System.Object put(int index, char c)
        {
            throw new global::org.xmlvm._nExceptionAdapter(new ReadOnlyBufferException());
        }

        public override global::System.Object put(global::org.xmlvm._nArrayAdapter<char> src, int off, int len)
        {
            if ((off < 0) || (len < 0) || (long)off + (long)len > src.Length)
            {
                throw new System.IndexOutOfRangeException();
            }

            if (len > remaining())
            {
                throw new global::org.xmlvm._nExceptionAdapter(new BufferOverflowException());
            }

            throw new global::org.xmlvm._nExceptionAdapter(new ReadOnlyBufferException());
        }

        public override global::System.Object put(global::java.lang.String src, int start, int end)
        {
            if ((start < 0) || (end < 0) || (long)start + (long)end > src.length())
            {
                throw new System.IndexOutOfRangeException();
            }
            throw new global::org.xmlvm._nExceptionAdapter(new ReadOnlyBufferException());
        }

        public override global::System.Object slice()
        {
            return new CharSequenceAdapter((java.lang.CharSequence)sequence.subSequence(_fposition, _flimit));
        }

        public override global::System.Object subSequence(int start, int end)
        {
            if (end < start || start < 0 || end > remaining())
            {
                throw new System.IndexOutOfRangeException();
            }

            CharSequenceAdapter result = copy(this);
            result._fposition = _fposition + start;
            result._flimit = _fposition + end;
            return result;
        }
    }
}
