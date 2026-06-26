/* OpenGLES shim for the macOS simulator build: the port headers import
 * EAGL types but the Metal build path never instantiates them. */
#ifndef CN1SIM_EAGL_SHIM_H
#define CN1SIM_EAGL_SHIM_H
#import <Foundation/Foundation.h>
typedef NSUInteger EAGLRenderingAPI;
enum { kEAGLRenderingAPIOpenGLES1 = 1, kEAGLRenderingAPIOpenGLES2 = 2 };
@interface EAGLContext : NSObject
@property (readonly) EAGLRenderingAPI API;
+ (BOOL)setCurrentContext:(EAGLContext *)context;
+ (EAGLContext *)currentContext;
- (id)initWithAPI:(EAGLRenderingAPI)api;
@end
@protocol EAGLDrawable
@end
#endif
