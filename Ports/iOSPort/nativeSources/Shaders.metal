//
//  Shaders.metal
//  HelloMetal
//
//  Created by Steve Hannah on 2019-07-25.
//  Copyright © 2019 CodenameOne. All rights reserved.
//

#include <metal_stdlib>
using namespace metal;

struct ColoredVertex
{
    float4 position [[position]];
    float4 color;
};

vertex ColoredVertex FillRect_vertex(constant float4 *position [[buffer(0)]],
                                 constant float4 *color [[buffer(1)]],
                                 uint vid [[vertex_id]])
{
    ColoredVertex vert;
    vert.position = position[vid];
    vert.color = color[vid];
    return vert;
}

fragment float4 FillRect_fragment(ColoredVertex vert [[stage_in]])
{
    return vert.color;
}
