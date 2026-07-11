/* Native virtual accessibility tree for the lightweight Win32 port. */
#ifdef _WIN32

#include "cn1_windows.h"
#include <UIAutomation.h>
#include <oleauto.h>
#include <string>
#include <vector>
#include <utility>
#include <algorithm>

#pragma comment(lib, "uiautomationcore.lib")
#pragma comment(lib, "oleaut32.lib")

struct CN1UiaNode {
    long long id, parent;
    long long actionTarget;
    std::wstring role, label, description, value;
    int x, y, width, height, flags;
    std::vector<std::pair<int, std::wstring> > actions;
};

static SRWLOCK cn1UiaLock = SRWLOCK_INIT;
static std::vector<CN1UiaNode> cn1UiaNodes;
static std::vector<CN1UiaNode> cn1UiaPending;

static std::wstring cn1UiaWide(const char* value) {
    if (!value) return std::wstring();
    int length = MultiByteToWideChar(CP_UTF8, 0, value, -1, NULL, 0);
    if (length <= 1) return std::wstring();
    std::vector<wchar_t> buffer((size_t) length);
    MultiByteToWideChar(CP_UTF8, 0, value, -1, &buffer[0], length);
    return std::wstring(&buffer[0]);
}

static bool cn1UiaGet(long long id, CN1UiaNode& out) {
    bool found = false;
    AcquireSRWLockShared(&cn1UiaLock);
    for (size_t i = 0; i < cn1UiaNodes.size(); ++i) {
        if (cn1UiaNodes[i].id == id) { out = cn1UiaNodes[i]; found = true; break; }
    }
    ReleaseSRWLockShared(&cn1UiaLock);
    return found;
}

static long long cn1UiaNavigate(long long id, NavigateDirection direction) {
    long long result = 0;
    AcquireSRWLockShared(&cn1UiaLock);
    if (id == 0) {
        if (direction == NavigateDirection_FirstChild || direction == NavigateDirection_LastChild) {
            for (size_t i = 0; i < cn1UiaNodes.size(); ++i) {
                size_t index = direction == NavigateDirection_FirstChild ? i : cn1UiaNodes.size() - 1 - i;
                if (cn1UiaNodes[index].parent < 0) { result = cn1UiaNodes[index].id; break; }
            }
        }
    } else {
        size_t current = cn1UiaNodes.size();
        for (size_t i = 0; i < cn1UiaNodes.size(); ++i) if (cn1UiaNodes[i].id == id) { current = i; break; }
        if (current < cn1UiaNodes.size()) {
            const CN1UiaNode& node = cn1UiaNodes[current];
            if (direction == NavigateDirection_Parent) result = node.parent < 0 ? 0 : node.parent;
            if (direction == NavigateDirection_FirstChild || direction == NavigateDirection_LastChild) {
                for (size_t i = 0; i < cn1UiaNodes.size(); ++i) {
                    size_t index = direction == NavigateDirection_FirstChild ? i : cn1UiaNodes.size() - 1 - i;
                    if (cn1UiaNodes[index].parent == id) { result = cn1UiaNodes[index].id; break; }
                }
            }
            if (direction == NavigateDirection_NextSibling) {
                for (size_t i = current + 1; i < cn1UiaNodes.size(); ++i)
                    if (cn1UiaNodes[i].parent == node.parent) { result = cn1UiaNodes[i].id; break; }
            }
            if (direction == NavigateDirection_PreviousSibling && current > 0) {
                for (size_t i = current; i-- > 0; )
                    if (cn1UiaNodes[i].parent == node.parent) { result = cn1UiaNodes[i].id; break; }
            }
        }
    }
    ReleaseSRWLockShared(&cn1UiaLock);
    return result;
}

static CONTROLTYPEID cn1UiaControlType(const std::wstring& role) {
    if (role == L"BUTTON" || role == L"TOGGLE_BUTTON") return UIA_ButtonControlTypeId;
    if (role == L"CHECKBOX" || role == L"SWITCH") return UIA_CheckBoxControlTypeId;
    if (role == L"RADIO_BUTTON") return UIA_RadioButtonControlTypeId;
    if (role == L"TEXT_FIELD" || role == L"SEARCH_FIELD") return UIA_EditControlTypeId;
    if (role == L"SLIDER") return UIA_SliderControlTypeId;
    if (role == L"PROGRESS_BAR") return UIA_ProgressBarControlTypeId;
    if (role == L"LIST") return UIA_ListControlTypeId;
    if (role == L"LIST_ITEM") return UIA_ListItemControlTypeId;
    if (role == L"GRID") return UIA_DataGridControlTypeId;
    if (role == L"ROW" || role == L"CELL") return UIA_DataItemControlTypeId;
    if (role == L"TAB_LIST") return UIA_TabControlTypeId;
    if (role == L"TAB") return UIA_TabItemControlTypeId;
    if (role == L"DIALOG" || role == L"ALERT") return UIA_WindowControlTypeId;
    if (role == L"MENU") return UIA_MenuControlTypeId;
    if (role == L"MENU_ITEM") return UIA_MenuItemControlTypeId;
    if (role == L"TOOLBAR") return UIA_ToolBarControlTypeId;
    if (role == L"SCROLL_BAR") return UIA_ScrollBarControlTypeId;
    if (role == L"COMBO_BOX") return UIA_ComboBoxControlTypeId;
    if (role == L"TREE") return UIA_TreeControlTypeId;
    if (role == L"TREE_ITEM") return UIA_TreeItemControlTypeId;
    if (role == L"IMAGE") return UIA_ImageControlTypeId;
    if (role == L"LINK") return UIA_HyperlinkControlTypeId;
    if (role == L"STATIC_TEXT" || role == L"HEADING") return UIA_TextControlTypeId;
    if (role == L"SEPARATOR") return UIA_SeparatorControlTypeId;
    return UIA_GroupControlTypeId;
}

class CN1UiaProvider : public IRawElementProviderSimple, public IRawElementProviderFragment,
        public IRawElementProviderFragmentRoot, public IInvokeProvider {
    LONG refs;
    long long id;
public:
    explicit CN1UiaProvider(long long nodeId) : refs(1), id(nodeId) {}
    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID iid, void** object) {
        if (!object) return E_INVALIDARG;
        *object = NULL;
        if (iid == __uuidof(IUnknown) || iid == __uuidof(IRawElementProviderSimple))
            *object = static_cast<IRawElementProviderSimple*>(this);
        else if (iid == __uuidof(IRawElementProviderFragment))
            *object = static_cast<IRawElementProviderFragment*>(this);
        else if (iid == __uuidof(IRawElementProviderFragmentRoot) && id == 0)
            *object = static_cast<IRawElementProviderFragmentRoot*>(this);
        else if (iid == __uuidof(IInvokeProvider))
            *object = static_cast<IInvokeProvider*>(this);
        if (!*object) return E_NOINTERFACE;
        AddRef(); return S_OK;
    }
    ULONG STDMETHODCALLTYPE AddRef() { return (ULONG) InterlockedIncrement(&refs); }
    ULONG STDMETHODCALLTYPE Release() { ULONG value = (ULONG) InterlockedDecrement(&refs); if (!value) delete this; return value; }

    HRESULT STDMETHODCALLTYPE get_ProviderOptions(ProviderOptions* options) {
        if (!options) return E_INVALIDARG; *options = ProviderOptions_ServerSideProvider; return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetPatternProvider(PATTERNID pattern, IUnknown** provider) {
        if (!provider) return E_INVALIDARG; *provider = NULL;
        CN1UiaNode node;
        if (pattern == UIA_InvokePatternId && id != 0 && cn1UiaGet(id, node) && !node.actions.empty()) {
            *provider = static_cast<IInvokeProvider*>(this); AddRef();
        }
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetPropertyValue(PROPERTYID property, VARIANT* value) {
        if (!value) return E_INVALIDARG;
        VariantInit(value);
        if (id == 0) {
            if (property == UIA_NamePropertyId) { value->vt = VT_BSTR; value->bstrVal = SysAllocString(L"Codename One"); }
            else if (property == UIA_ControlTypePropertyId) { value->vt = VT_I4; value->lVal = UIA_WindowControlTypeId; }
            else if (property == UIA_IsControlElementPropertyId || property == UIA_IsContentElementPropertyId) { value->vt = VT_BOOL; value->boolVal = VARIANT_TRUE; }
            return S_OK;
        }
        CN1UiaNode node; if (!cn1UiaGet(id, node)) return UIA_E_ELEMENTNOTAVAILABLE;
        if (property == UIA_NamePropertyId) { value->vt = VT_BSTR; value->bstrVal = SysAllocString(node.label.c_str()); }
        else if (property == UIA_HelpTextPropertyId) { value->vt = VT_BSTR; value->bstrVal = SysAllocString(node.description.c_str()); }
        else if (property == UIA_ValueValuePropertyId) { value->vt = VT_BSTR; value->bstrVal = SysAllocString(node.value.c_str()); }
        else if (property == UIA_ControlTypePropertyId) { value->vt = VT_I4; value->lVal = cn1UiaControlType(node.role); }
        else if (property == UIA_LocalizedControlTypePropertyId) { value->vt = VT_BSTR; value->bstrVal = SysAllocString(node.role.c_str()); }
        else if (property == UIA_IsEnabledPropertyId) { value->vt = VT_BOOL; value->boolVal = (node.flags & 4) ? VARIANT_TRUE : VARIANT_FALSE; }
        else if (property == UIA_HasKeyboardFocusPropertyId) { value->vt = VT_BOOL; value->boolVal = (node.flags & 2) ? VARIANT_TRUE : VARIANT_FALSE; }
        else if (property == UIA_IsKeyboardFocusablePropertyId) { value->vt = VT_BOOL; value->boolVal = (node.flags & 1) ? VARIANT_TRUE : VARIANT_FALSE; }
        else if (property == UIA_IsControlElementPropertyId || property == UIA_IsContentElementPropertyId) { value->vt = VT_BOOL; value->boolVal = VARIANT_TRUE; }
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE get_HostRawElementProvider(IRawElementProviderSimple** provider) {
        if (!provider) return E_INVALIDARG; *provider = NULL;
        return id == 0 && cn1Win.hwnd ? UiaHostProviderFromHwnd(cn1Win.hwnd, provider) : S_OK;
    }
    HRESULT STDMETHODCALLTYPE Navigate(NavigateDirection direction, IRawElementProviderFragment** result) {
        if (!result) return E_INVALIDARG; *result = NULL;
        long long target = cn1UiaNavigate(id, direction);
        if (target || (direction == NavigateDirection_Parent && id != 0)) *result = new CN1UiaProvider(target);
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetRuntimeId(SAFEARRAY** runtimeId) {
        if (!runtimeId) return E_INVALIDARG; *runtimeId = NULL;
        if (id == 0) return S_OK;
        SAFEARRAY* array = SafeArrayCreateVector(VT_I4, 0, 3); if (!array) return E_OUTOFMEMORY;
        LONG indexes[] = {0, 1, 2}; int values[] = {UiaAppendRuntimeId, (int)(id >> 32), (int)id};
        for (int i = 0; i < 3; ++i) SafeArrayPutElement(array, &indexes[i], &values[i]);
        *runtimeId = array; return S_OK;
    }
    HRESULT STDMETHODCALLTYPE get_BoundingRectangle(UiaRect* rectangle) {
        if (!rectangle) return E_INVALIDARG;
        if (id == 0) { RECT r; GetWindowRect(cn1Win.hwnd, &r); rectangle->left=r.left; rectangle->top=r.top; rectangle->width=r.right-r.left; rectangle->height=r.bottom-r.top; return S_OK; }
        CN1UiaNode node; if (!cn1UiaGet(id, node)) return UIA_E_ELEMENTNOTAVAILABLE;
        POINT point = {node.x, node.y}; ClientToScreen(cn1Win.hwnd, &point);
        rectangle->left=point.x; rectangle->top=point.y; rectangle->width=node.width; rectangle->height=node.height; return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetEmbeddedFragmentRoots(SAFEARRAY** roots) { if (!roots) return E_INVALIDARG; *roots=NULL; return S_OK; }
    HRESULT STDMETHODCALLTYPE SetFocus() { return S_OK; }
    HRESULT STDMETHODCALLTYPE get_FragmentRoot(IRawElementProviderFragmentRoot** root) {
        if (!root) return E_INVALIDARG; *root = new CN1UiaProvider(0); return S_OK;
    }
    HRESULT STDMETHODCALLTYPE ElementProviderFromPoint(double x, double y, IRawElementProviderFragment** result) {
        if (!result) return E_INVALIDARG; *result=NULL; POINT p={(LONG)x,(LONG)y}; ScreenToClient(cn1Win.hwnd,&p);
        long long found=0; AcquireSRWLockShared(&cn1UiaLock);
        for (size_t i=0;i<cn1UiaNodes.size();++i) { const CN1UiaNode& n=cn1UiaNodes[i]; if(p.x>=n.x&&p.y>=n.y&&p.x<n.x+n.width&&p.y<n.y+n.height) found=n.id; }
        ReleaseSRWLockShared(&cn1UiaLock); if(found) *result=new CN1UiaProvider(found); return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetFocus(IRawElementProviderFragment** result) {
        if (!result) return E_INVALIDARG; *result=NULL; AcquireSRWLockShared(&cn1UiaLock);
        for(size_t i=0;i<cn1UiaNodes.size();++i) if(cn1UiaNodes[i].flags&2){*result=new CN1UiaProvider(cn1UiaNodes[i].id);break;}
        ReleaseSRWLockShared(&cn1UiaLock); return S_OK;
    }
    HRESULT STDMETHODCALLTYPE Invoke() {
        CN1UiaNode node; if (!cn1UiaGet(id,node)) return UIA_E_ELEMENTNOTAVAILABLE;
        if(node.actions.empty()) return UIA_E_NOTSUPPORTED;
        cn1WinPushEvent(CN1_EVENT_ACCESSIBILITY_ACTION,(int)node.actionTarget,0,node.actions[0].first); return S_OK;
    }
};

extern "C" LRESULT cn1WinAccessibilityObject(HWND hwnd, WPARAM wParam, LPARAM lParam) {
    if ((LONG) lParam != UiaRootObjectId) return DefWindowProcW(hwnd, WM_GETOBJECT, wParam, lParam);
    CN1UiaProvider* provider = new CN1UiaProvider(0);
    LRESULT result = UiaReturnRawElementProvider(hwnd, wParam, lParam, provider);
    provider->Release(); return result;
}

extern "C" {
JAVA_VOID com_codename1_impl_windows_WindowsNative_accessibilityBegin__(CODENAME_ONE_THREAD_STATE) {
    AcquireSRWLockExclusive(&cn1UiaLock); cn1UiaPending.clear(); ReleaseSRWLockExclusive(&cn1UiaLock);
}
JAVA_VOID com_codename1_impl_windows_WindowsNative_accessibilityNode___long_long_java_lang_String_java_lang_String_java_lang_String_java_lang_String_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG id, JAVA_LONG parent, JAVA_OBJECT role, JAVA_OBJECT label,
        JAVA_OBJECT description, JAVA_OBJECT value, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_INT flags) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    CN1UiaNode node; node.id=id; node.parent=parent; node.actionTarget=id; node.x=x; node.y=y; node.width=width; node.height=height; node.flags=flags;
    node.role=role==JAVA_NULL?L"":cn1UiaWide(stringToUTF8(threadStateData,role));
    node.label=label==JAVA_NULL?L"":cn1UiaWide(stringToUTF8(threadStateData,label));
    node.description=description==JAVA_NULL?L"":cn1UiaWide(stringToUTF8(threadStateData,description));
    node.value=value==JAVA_NULL?L"":cn1UiaWide(stringToUTF8(threadStateData,value));
    AcquireSRWLockExclusive(&cn1UiaLock); cn1UiaPending.push_back(node); ReleaseSRWLockExclusive(&cn1UiaLock);
}
JAVA_VOID com_codename1_impl_windows_WindowsNative_accessibilityAction___long_java_lang_String_int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG nodeId, JAVA_OBJECT actionId, JAVA_INT actionHash, JAVA_OBJECT label) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    std::wstring idText=actionId==JAVA_NULL?L"":cn1UiaWide(stringToUTF8(threadStateData,actionId));
    std::wstring text=label==JAVA_NULL?L"":cn1UiaWide(stringToUTF8(threadStateData,label));
    AcquireSRWLockExclusive(&cn1UiaLock);
    for(size_t i=0;i<cn1UiaPending.size();++i) if(cn1UiaPending[i].id==nodeId){
        if(idText==L"activate" || idText==L"focus" || idText==L"increment" || idText==L"decrement") {
            cn1UiaPending[i].actions.push_back(std::make_pair((int)actionHash,text));
        } else {
            CN1UiaNode actionNode;
            actionNode.id = -(((long long)(unsigned int)actionHash << 32) ^ nodeId ^ 0x43a11L);
            actionNode.parent=nodeId; actionNode.actionTarget=nodeId; actionNode.role=L"BUTTON";
            actionNode.label=text.empty()?idText:text; actionNode.description=L"Custom accessibility action";
            actionNode.value=L""; actionNode.x=cn1UiaPending[i].x; actionNode.y=cn1UiaPending[i].y;
            actionNode.width=cn1UiaPending[i].width; actionNode.height=cn1UiaPending[i].height;
            actionNode.flags=5; actionNode.actions.push_back(std::make_pair((int)actionHash,text));
            cn1UiaPending.push_back(actionNode);
        }
        break;
    }
    ReleaseSRWLockExclusive(&cn1UiaLock);
}
JAVA_VOID com_codename1_impl_windows_WindowsNative_accessibilityEnd___int(CODENAME_ONE_THREAD_STATE, JAVA_INT changeType) {
    AcquireSRWLockExclusive(&cn1UiaLock); cn1UiaNodes.swap(cn1UiaPending); cn1UiaPending.clear(); ReleaseSRWLockExclusive(&cn1UiaLock);
    CN1UiaProvider* root=new CN1UiaProvider(0);
    StructureChangeType type=(changeType&1)?StructureChangeType_ChildrenInvalidated:StructureChangeType_ChildrenReordered;
    UiaRaiseStructureChangedEvent(root,type,NULL,0); root->Release();
}
}

#endif
