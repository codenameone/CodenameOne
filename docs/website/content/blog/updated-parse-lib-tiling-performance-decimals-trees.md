---
title: Updated Parse Lib, Tiling Performance, Decimals & Trees
slug: updated-parse-lib-tiling-performance-decimals-trees
url: /blog/updated-parse-lib-tiling-performance-decimals-trees/
original_url: https://www.codenameone.com/blog/updated-parse-lib-tiling-performance-decimals-trees.html
aliases:
- /blog/updated-parse-lib-tiling-performance-decimals-trees.html
date: '2015-09-22'
author: Shai Almog
---

![Header Image](/blog/updated-parse-lib-tiling-performance-decimals-trees/parse.com-post-header.jpg)

The excellent [  
Parse4cn1 library just announced version 1.1.](https://github.com/sidiabale/parse4cn1/releases/tag/parse4cn1-1.1) The biggest new feature is batch operations but there are  
a few others that could be helpful. Overall the library was pretty solid before the 1.1 version and this is a nice  
improvement on top. 

Fabricio Cabeca contributed an interesting  
[pull request](https://github.com/codenameone/CodenameOne/pull/1580)  
that has the potential to improve performance nicely on most platforms. Hopefully we’ll fix the issues in this  
approach and get it up again. 

Steve reimplemented our `BigDecimal` & `BigInteger` based on the TeaVM  
implementation to make it more compliant to the Java SE version of these classes. 

#### Expanding A Tree

A common request for the tree class is to open it when its already expanded. I recently had to do that myself for  
the GUI builder and noticed that one of the pain points is the fact that the `Tree` always animates  
expansion. So we added a new method that allows path expansion without animation specifically: `expandPath(animated, path)`.  
This tree class shows itself expanded by default and can be useful for your app: 
    
    
    public class MyTree extends Tree {
        public MyTree(TreeModel model) {
            super(model);
        }
    
        @Override
        protected void initComponent() {
            expand();
            super.initComponent(); 
        }
    
        public void expand() {
            Object current = null;
            Vector c = getModel().getChildren(current);
            if(c != null) {
                for(Object o : c) {
                    recurseExpand(o);
                }
            }
        }
        
        private void recurseExpand(Object... path) {
            expandPath(false, path);
            if(getModel().isLeaf(path[path.length - 1])) {
                return;
            }
            Vector ch = getModel().getChildren(path[path.length - 1]);
            if(ch != null) {
                Object[] arr = new Object[path.length + 1];
                for(int iter = 0 ; iter < path.length ; iter++) {
                    arr[iter] = path[iter];
                }
                for(Object o : ch) {
                    arr[path.length] = o;
                    recurseExpand(arr);
                }
            }
        }
    }

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
