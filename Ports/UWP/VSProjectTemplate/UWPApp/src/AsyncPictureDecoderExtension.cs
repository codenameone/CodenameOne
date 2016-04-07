/*
 * Copyright 2012 ZXing.Net authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System.ComponentModel;
using Windows.Phone.Media;
using com.codename1.codescan;
using com.codename1.impl;
using Windows.Media.Capture;
using Windows.UI.Xaml.Media.Imaging;
using com.codename1.processing;
using Windows.ApplicationModel.Background;
using System.Threading.Tasks;
using Microsoft.Graphics.Canvas;

namespace WindowsPhone8Demo.Extensions
{
    public class AsyncPictureDecoderExtension : java.lang.Runnable
    {

        private BitmapImage _image;
        private ZXing.BarcodeReader _reader;
        private ScanResult result;
        public AsyncPictureDecoderExtension(ScanResult result, PhotoConfirmationCapturedEventArgs photoResult)
        {
            this.result = result;

            InitializeDecoder(photoResult);
        }


        private void InitializeDecoder(PhotoConfirmationCapturedEventArgs photoResult)
        {

            _image = new BitmapImage();
            _reader = new ZXing.BarcodeReader();


            Task.Run(delegate ()
            {
                _reader.Options.TryHarder = false;
                _reader.TryInverted = false;
                _reader.AutoRotate = false;
                _image.SetSource(photoResult.Frame);
                ZXing.Result result = _reader.Decode(new WriteableBitmap(_image.PixelWidth, _image.PixelHeight));
                if (result != null)
                {
                    AddBarcodeToResults(result);
                }
                else
                {
                    SetActivityMessage("No barcode detected", false);
                }
            }).ConfigureAwait(false).GetAwaiter().GetResult();
        }

        string contents;
        string format;
        byte[] raw;
        public void run()
        {
            result.scanCompleted(contents, format, raw);
        }
        private void SetActivityMessage(string msg, bool processing)
        {
            //_viewModel.ActivityMessage = msg;
            //_viewModel.Processing = processing;
        }

        private void AddBarcodeToResults(ZXing.Result result)
        {

            SetActivityMessage("Barcode decoded", false);
            //BarcodeResult.AddToResultCollection(result, _viewModel);
            format = result.BarcodeFormat.ToString();
            contents = result.Text;
            raw = result.RawBytes;
            com.codename1.ui.Display d = (com.codename1.ui.Display)com.codename1.ui.Display.getInstance();
            d.callSerially(this);
        }
    }
}