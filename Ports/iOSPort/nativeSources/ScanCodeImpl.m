//
//  ScanCodeImpl.m
//  KitchenSink
//
//  Created by Shai Almog on 05/11/12.
//
//

#import "ScanCodeImpl.h"
//#import "QRCodeReaderOC.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "xmlvm.h"


@implementation ScanCodeImpl
#if !TARGET_IPHONE_SIMULATOR
- (void) imagePickerController: (UIImagePickerController*) reader didFinishPickingMediaWithInfo: (NSDictionary*) info
{
        // ADD: get the decode results
        id<NSFastEnumeration> results = [info objectForKey: ZBarReaderControllerResults];
        ZBarSymbol *symbol = nil;
        for(symbol in results)
            // EXAMPLE: just grab the first barcode
            break;
        
        // EXAMPLE: do something useful with the barcode data
        //resultText.text = symbol.data;
        
        // EXAMPLE: do something useful with the barcode image
        //resultImage.image = [info objectForKey: UIImagePickerControllerOriginalImage];
        
        // ADD: dismiss the controller (NB dismiss from the *reader*!)
        [reader dismissModalViewControllerAnimated: YES];
        
        com_codename1_impl_ios_IOSImplementation_scanCompleted___java_lang_String_java_lang_String(fromNSString(symbol.data), fromNSString(symbol.typeName));        
}
#endif

/*- (void)zxingController:(ZXingWidgetController*)controller didScanResult:(NSString *)result {
    com_codename1_impl_ios_IOSImplementation_scanCompleted___java_lang_String_java_lang_String(fromNSString(result), nil);
    [[CodenameOne_GLViewController instance] dismissModalViewControllerAnimated:NO];
}

- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller {
    com_codename1_impl_ios_IOSImplementation_scanCanceled__();
    [[CodenameOne_GLViewController instance] dismissModalViewControllerAnimated:YES];
}*/

@end
