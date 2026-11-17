/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.brickyard.rbx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Every engine class: its superclass, property defaults, events and method
/// names. Behaviour (what a method does, computed properties) lives in a
/// [Behavior] attached to the class; this file is data.
///
/// One line per class: `Name:Super;flags;props;events;methods`, where flags
/// are `A` (not creatable with `Instance.new`) and `S` (a service), props are
/// `Name=value` separated by `|`, and a value is typed by its prefix:
/// `b:` bool, `n:` number, `s:` string, `v3:` Vector3, `v2:` Vector2,
/// `c3:` Color3 from 0..255 channels, `u2:` UDim2, `u:` UDim, `cf` identity
/// CFrame, `E:Type.Item` enum, `R` instance reference, `bc:` BrickColor
/// number, `nr:` NumberRange, `ns:` constant NumberSequence, `cs:` constant
/// ColorSequence.
public final class ClassDb {
    /// Marks an instance-reference property (default nil).
    public static final Object REF = new Object();

    private static final String[] DEFS = {
        "Instance:;A;Name=s:Instance|Archivable=b:true;Changed,ChildAdded,ChildRemoved,DescendantAdded,"
            + "DescendantRemoving,AncestryChanged,Destroying,AttributeChanged;",
        "ServiceProvider:Instance;A;;;GetService,FindService",
        "DataModel:ServiceProvider;A;PlaceId=n:0|GameId=n:0|JobId=s:|CreatorId=n:0|PlaceVersion=n:1|"
            + "PrivateServerId=s:|PrivateServerOwnerId=n:0;Close;BindToClose,IsLoaded",
        "PVInstance:Instance;A;;;GetPivot,PivotTo",
        "Model:PVInstance;;PrimaryPart=R|WorldPivot=cf|LevelOfDetail=E:Font.Legacy;;GetBoundingBox,"
            + "GetExtentsSize,MoveTo,SetPrimaryPartCFrame,GetPrimaryPartCFrame,ScaleTo,GetScale,TranslateBy",
        "WorldRoot:Model;A;;;Raycast,Blockcast,Spherecast,Shapecast,GetPartBoundsInBox,GetPartBoundsInRadius,"
            + "GetPartsInPart,FindPartOnRay,FindPartOnRayWithIgnoreList,FindPartsInRegion3,BulkMoveTo",
        "Workspace:WorldRoot;AS;Gravity=n:196.2|CurrentCamera=R|FallenPartsDestroyHeight=n:-500|"
            + "DistributedGameTime=n:0|StreamingEnabled=b:false|AirDensity=n:0.0012;;GetServerTimeNow,GetRealPhysicsFPS",
        "WorldModel:WorldRoot;;;;",
        "BasePart:PVInstance;A;CFrame=cf|Size=v3:4,1,2|Anchored=b:false|CanCollide=b:true|CanTouch=b:true|"
            + "CanQuery=b:true|Transparency=n:0|Reflectance=n:0|Color=c3:163,162,165|Material=E:Material.Plastic|"
            + "Massless=b:false|Locked=b:false|CastShadow=b:true|AssemblyLinearVelocity=v3:0,0,0|"
            + "AssemblyAngularVelocity=v3:0,0,0|CollisionGroup=s:Default|RootPriority=n:0|"
            + "TopSurface=E:SurfaceType.Smooth|BottomSurface=E:SurfaceType.Smooth|MaterialVariant=s:|"
            + "CustomPhysicalProperties=R;Touched,TouchEnded;GetTouchingParts,GetMass,ApplyImpulse,"
            + "ApplyAngularImpulse,ApplyImpulseAtPosition,GetConnectedParts,BreakJoints,GetRootPart,CanCollideWith,"
            + "SetNetworkOwner,GetNetworkOwner,SetNetworkOwnershipAuto,IsGrounded,GetVelocityAtPosition,"
            + "GetJoints,Resize,SubtractAsync,UnionAsync,IntersectAsync",
        "FormFactorPart:BasePart;A;;;",
        "Part:FormFactorPart;;Shape=E:PartType.Block;;",
        "WedgePart:FormFactorPart;;;;",
        "CornerWedgePart:BasePart;;;;",
        "TrussPart:BasePart;;Size=v3:2,10,2;;",
        "MeshPart:BasePart;;MeshId=s:|TextureID=s:|DoubleSided=b:false|RenderFidelity=E:Font.Legacy;;ApplyMesh",
        "PartOperation:BasePart;A;UsePartColor=b:false;;",
        "UnionOperation:PartOperation;;;;",
        "NegateOperation:PartOperation;;;;",
        "IntersectOperation:PartOperation;;;;",
        "SpawnLocation:Part;;Neutral=b:true|TeamColor=bc:194|AllowTeamChangeOnTouch=b:false|Duration=n:10|"
            + "Enabled=b:true;;",
        "Seat:Part;;Disabled=b:false|Occupant=R;;Sit",
        "VehicleSeat:BasePart;;Disabled=b:false|Occupant=R|MaxSpeed=n:25|Torque=n:10|TurnSpeed=n:1|"
            + "Throttle=n:0|Steer=n:0|ThrottleFloat=n:0|SteerFloat=n:0|HeadsUpDisplay=b:true;;Sit",
        "Terrain:BasePart;A;WaterColor=c3:12,84,92|WaterTransparency=n:0.3|WaterWaveSize=n:0.15|"
            + "WaterWaveSpeed=n:10|WaterReflectance=n:1|Decoration=b:false;;FillBlock,FillBall,FillCylinder,"
            + "FillWedge,FillRegion,Clear,ReadVoxels,WriteVoxels,CellCenterToWorld,WorldToCell,GetMaterialColor,"
            + "SetMaterialColor",
        "FaceInstance:Instance;A;Face=E:NormalId.Front;;",
        "Decal:FaceInstance;;Texture=s:|Color3=c3:255,255,255|Transparency=n:0|ZIndex=n:1;;",
        "Texture:Decal;;StudsPerTileU=n:2|StudsPerTileV=n:2|OffsetStudsU=n:0|OffsetStudsV=n:0;;",
        "Attachment:Instance;;CFrame=cf|Visible=b:false|Axis=v3:1,0,0|SecondaryAxis=v3:0,1,0;;GetConstraints",
        "Bone:Attachment;;Transform=cf;;",
        "JointInstance:Instance;A;Part0=R|Part1=R|C0=cf|C1=cf|Enabled=b:true|Active=b:true;;",
        "Weld:JointInstance;;;;",
        "Snap:JointInstance;;;;",
        "ManualWeld:JointInstance;;;;",
        "Motor:JointInstance;;CurrentAngle=n:0|DesiredAngle=n:0|MaxVelocity=n:0;;SetDesiredAngle",
        "Motor6D:Motor;;Transform=cf;;",
        "WeldConstraint:Instance;;Part0=R|Part1=R|Enabled=b:true|Active=b:true;;",
        "NoCollisionConstraint:Instance;;Part0=R|Part1=R|Enabled=b:true;;",
        "Constraint:Instance;A;Attachment0=R|Attachment1=R|Enabled=b:true|Visible=b:false|Color=bc:23|Active=b:true;;",
        "RopeConstraint:Constraint;;Length=n:5|Restitution=n:0|Thickness=n:0.1|WinchEnabled=b:false;;",
        "RodConstraint:Constraint;;Length=n:5|Thickness=n:0.1;;",
        "SpringConstraint:Constraint;;FreeLength=n:1|Stiffness=n:0|Damping=n:0|LimitsEnabled=b:false|"
            + "MaxLength=n:5|MinLength=n:0;;",
        "HingeConstraint:Constraint;;ActuatorType=E:ActuatorType.None|AngularVelocity=n:0|MotorMaxTorque=n:0|"
            + "TargetAngle=n:0|AngularSpeed=n:0|ServoMaxTorque=n:0|LimitsEnabled=b:false|UpperAngle=n:45|"
            + "LowerAngle=n:-45|CurrentAngle=n:0;;",
        "BallSocketConstraint:Constraint;;LimitsEnabled=b:false|UpperAngle=n:45;;",
        "PrismaticConstraint:Constraint;;ActuatorType=E:ActuatorType.None|Velocity=n:0|Speed=n:0|"
            + "TargetPosition=n:0|LimitsEnabled=b:false|UpperLimit=n:5|LowerLimit=n:0|CurrentPosition=n:0;;",
        "AlignPosition:Constraint;;Position=v3:0,0,0|MaxForce=n:10000|MaxVelocity=n:1000000|Responsiveness=n:10|"
            + "RigidityEnabled=b:false|Mode=E:ActuatorType.None;;",
        "AlignOrientation:Constraint;;CFrame=cf|MaxTorque=n:10000|MaxAngularVelocity=n:1000000|"
            + "Responsiveness=n:10|RigidityEnabled=b:false;;",
        "LinearVelocity:Constraint;;VectorVelocity=v3:0,0,0|MaxForce=n:1000|LineVelocity=n:0|"
            + "RelativeTo=E:ActuatorRelativeTo.World;;",
        "AngularVelocity:Constraint;;AngularVelocity=v3:0,0,0|MaxTorque=n:0|RelativeTo=E:ActuatorRelativeTo.World;;",
        "VectorForce:Constraint;;Force=v3:0,0,0|ApplyAtCenterOfMass=b:false|RelativeTo=E:ActuatorRelativeTo.Attachment0;;",
        "Torque:Constraint;;Torque=v3:0,0,0;;",
        "BodyMover:Instance;A;;;",
        "BodyVelocity:BodyMover;;Velocity=v3:0,2,0|MaxForce=v3:4000,4000,4000|P=n:1250;;",
        "BodyPosition:BodyMover;;Position=v3:0,50,0|MaxForce=v3:4000,4000,4000|P=n:10000|D=n:1250;;",
        "BodyGyro:BodyMover;;CFrame=cf|MaxTorque=v3:400000,0,400000|P=n:3000|D=n:500;;",
        "BodyForce:BodyMover;;Force=v3:0,0,0;;",
        "BodyAngularVelocity:BodyMover;;AngularVelocity=v3:0,2,0|MaxTorque=v3:4000,4000,4000;;",
        "Humanoid:Instance;;Health=n:100|MaxHealth=n:100|WalkSpeed=n:16|JumpPower=n:50|JumpHeight=n:7.2|"
            + "UseJumpPower=b:true|HipHeight=n:2|AutoRotate=b:true|AutoJumpEnabled=b:true|DisplayName=s:|"
            + "MoveDirection=v3:0,0,0|Jump=b:false|Sit=b:false|PlatformStand=b:false|RootPart=R|SeatPart=R|"
            + "FloorMaterial=E:Material.Air|RigType=E:HumanoidRigType.R15|WalkToPoint=v3:0,0,0|WalkToPart=R|"
            + "MaxSlopeAngle=n:89|BreakJointsOnDeath=b:true|RequiresNeck=b:true|NameDisplayDistance=n:100|"
            + "HealthDisplayDistance=n:100|CameraOffset=v3:0,0,0|EvaluateStateMachine=b:true;Died,HealthChanged,"
            + "Running,Jumping,StateChanged,Touched,MoveToFinished,FreeFalling,Seated,Climbing,GettingUp,"
            + "FallingDown,Swimming,AnimationPlayed;TakeDamage,Move,MoveTo,ChangeState,GetState,LoadAnimation,"
            + "EquipTool,UnequipTools,GetPlayingAnimationTracks,SetStateEnabled,GetStateEnabled,"
            + "GetAppliedDescription,ApplyDescription,AddAccessory,GetAccessories,RemoveAccessories,BuildRigFromAttachments",
        "HumanoidDescription:Instance;;HeadColor=c3:234,184,146|TorsoColor=c3:13,105,172|"
            + "LeftArmColor=c3:234,184,146|RightArmColor=c3:234,184,146|LeftLegColor=c3:40,127,71|"
            + "RightLegColor=c3:40,127,71|HatAccessory=s:|FaceAccessory=s:|Shirt=n:0|Pants=n:0|Face=n:0|"
            + "BodyTypeScale=n:0.3|HeightScale=n:1|WidthScale=n:1|HeadScale=n:1|DepthScale=n:1;;",
        "Animator:Instance;;;AnimationPlayed;LoadAnimation,GetPlayingAnimationTracks",
        "Animation:Instance;;AnimationId=s:;;",
        "AnimationTrack:Instance;A;Animation=R|IsPlaying=b:false|Length=n:1|Looped=b:false|Speed=n:1|"
            + "TimePosition=n:0|WeightCurrent=n:0|WeightTarget=n:1|Priority=E:AnimationPriority.Core;"
            + "Stopped,Ended,DidLoop,KeyframeReached;Play,Stop,AdjustSpeed,AdjustWeight,GetMarkerReachedSignal,"
            + "GetTimeOfKeyframe",
        "KeyframeSequence:Instance;;Loop=b:true|Priority=E:AnimationPriority.Action;;",
        "Keyframe:Instance;;Time=n:0;;",
        "Pose:Instance;;CFrame=cf|Weight=n:1|EasingStyle=E:PoseEasingStyle.Linear;;",
        "Accessory:Instance;;AttachmentPoint=cf;;",
        "Hat:Accessory;;;;",
        "Clothing:Instance;A;Color3=c3:255,255,255;;",
        "Shirt:Clothing;;ShirtTemplate=s:;;",
        "Pants:Clothing;;PantsTemplate=s:;;",
        "ShirtGraphic:Instance;;Graphic=s:|Color3=c3:255,255,255;;",
        "BodyColors:Instance;;HeadColor3=c3:234,184,146|TorsoColor3=c3:13,105,172|LeftArmColor3=c3:234,184,146|"
            + "RightArmColor3=c3:234,184,146|LeftLegColor3=c3:40,127,71|RightLegColor3=c3:40,127,71;;",
        "CharacterMesh:Instance;;;;",
        "SpecialMesh:Instance;;MeshType=E:MeshType.Head|MeshId=s:|TextureId=s:|Scale=v3:1,1,1|Offset=v3:0,0,0|"
            + "VertexColor=v3:1,1,1;;",
        "BlockMesh:Instance;;Scale=v3:1,1,1|Offset=v3:0,0,0;;",
        "CylinderMesh:Instance;;Scale=v3:1,1,1|Offset=v3:0,0,0;;",
        "LuaSourceContainer:Instance;A;;;",
        "BaseScript:LuaSourceContainer;A;Disabled=b:false|Enabled=b:true|RunContext=E:Font.Legacy|"
            + "LinkedSource=s:;;",
        "Script:BaseScript;;Source=s:;;",
        "LocalScript:Script;;;;",
        "ModuleScript:LuaSourceContainer;;Source=s:;;",
        "Folder:Instance;;;;",
        "TextFilterResult:Instance;A;;;GetChatForUserAsync,GetNonChatStringForBroadcastAsync,"
            + "GetNonChatStringForUserAsync",
        "Translator:Instance;A;LocaleId=s:en-us;;Translate,FormatByKey",
        "Configuration:Instance;;;;",
        "ValueBase:Instance;A;;;",
        "StringValue:ValueBase;;Value=s:;;",
        "IntValue:ValueBase;;Value=n:0;;",
        "NumberValue:ValueBase;;Value=n:0;;",
        "BoolValue:ValueBase;;Value=b:false;;",
        "ObjectValue:ValueBase;;Value=R;;",
        "Vector3Value:ValueBase;;Value=v3:0,0,0;;",
        "CFrameValue:ValueBase;;Value=cf;;",
        "Color3Value:ValueBase;;Value=c3:0,0,0;;",
        "BrickColorValue:ValueBase;;Value=bc:194;;",
        "RayValue:ValueBase;;Value=R;;",
        "IntConstrainedValue:ValueBase;;Value=n:0|MinValue=n:0|MaxValue=n:10;;",
        "DoubleConstrainedValue:ValueBase;;Value=n:0|MinValue=n:0|MaxValue=n:1;;",
        "BaseRemoteEvent:Instance;A;;OnServerEvent,OnClientEvent;FireServer,FireClient,FireAllClients",
        "RemoteEvent:BaseRemoteEvent;;;;",
        "UnreliableRemoteEvent:BaseRemoteEvent;;;;",
        "RemoteFunction:Instance;;OnServerInvoke=R|OnClientInvoke=R;;InvokeServer,InvokeClient",
        "BindableEvent:Instance;;;Event;Fire",
        "BindableFunction:Instance;;OnInvoke=R;;Invoke",
        "Players:Instance;AS;LocalPlayer=R|MaxPlayers=n:50|PreferredPlayers=n:50|RespawnTime=n:5|"
            + "CharacterAutoLoads=b:true|BubbleChat=b:true|ClassicChat=b:false;PlayerAdded,PlayerRemoving,"
            + "PlayerMembershipChanged;GetPlayers,GetPlayerFromCharacter,GetPlayerByUserId,GetUserIdFromNameAsync,"
            + "GetNameFromUserIdAsync,GetUserThumbnailAsync,GetFriendsAsync,GetCharacterAppearanceAsync,"
            + "GetHumanoidDescriptionFromUserId,CreateHumanoidModelFromDescription,BanAsync,UnbanAsync,GetBanHistoryAsync",
        "Player:Instance;A;UserId=n:0|DisplayName=s:|Character=R|Team=R|TeamColor=bc:194|Neutral=b:true|"
            + "AccountAge=n:0|MembershipType=E:Font.Legacy|CameraMode=E:Font.Legacy|CameraMaxZoomDistance=n:128|"
            + "CameraMinZoomDistance=n:0.5|RespawnLocation=R|AutoJumpEnabled=b:true|HealthDisplayDistance=n:100|"
            + "NameDisplayDistance=n:100|LocaleId=s:en-us|DevComputerMovementMode=E:DevComputerMovementMode.UserChoice;"
            + "CharacterAdded,CharacterRemoving,CharacterAppearanceLoaded,Chatted,Idled,OnTeleport;LoadCharacter,"
            + "Kick,GetMouse,IsFriendsWith,GetRankInGroup,GetRoleInGroup,IsInGroup,GetJoinData,HasAppearanceLoaded,"
            + "DistanceFromCharacter,ClearCharacterAppearance,GetFriendsOnline,RequestStreamAroundAsync",
        "Mouse:Instance;A;Hit=cf|Target=R|X=n:0|Y=n:0|UnitRay=R|TargetFilter=R|Icon=s:|ViewSizeX=n:0|ViewSizeY=n:0;"
            + "Button1Down,Button1Up,Button2Down,Button2Up,Move,Idle,WheelForward,WheelBackward,KeyDown,KeyUp;",
        "PlayerGui:Instance;A;;;GetGuiObjectsAtPosition,SetTopbarTransparency",
        "Backpack:Instance;;;;",
        "PlayerScripts:Instance;A;;;",
        "StarterGear:Instance;;;;",
        "Teams:Instance;AS;;;GetTeams",
        "Team:Instance;;TeamColor=bc:1|AutoAssignable=b:true;PlayerAdded,PlayerRemoved;GetPlayers",
        "Lighting:Instance;AS;Ambient=c3:70,70,70|Brightness=n:2|ClockTime=n:14|ColorShift_Bottom=c3:0,0,0|"
            + "ColorShift_Top=c3:0,0,0|EnvironmentDiffuseScale=n:1|EnvironmentSpecularScale=n:1|"
            + "ExposureCompensation=n:0|FogColor=c3:192,192,192|FogEnd=n:100000|FogStart=n:0|"
            + "GeographicLatitude=n:41.733|GlobalShadows=b:true|OutdoorAmbient=c3:128,128,128|"
            + "ShadowSoftness=n:0.2|Technology=E:Technology.Future;LightingChanged;GetSunDirection,"
            + "GetMoonDirection,GetMinutesAfterMidnight,SetMinutesAfterMidnight",
        "Atmosphere:Instance;;Density=n:0.3|Offset=n:0|Color=c3:199,199,199|Decay=c3:106,112,125|Glare=n:0|Haze=n:0;;",
        "Sky:Instance;;SkyboxBk=s:|SkyboxDn=s:|SkyboxFt=s:|SkyboxLf=s:|SkyboxRt=s:|SkyboxUp=s:|"
            + "CelestialBodiesShown=b:true|StarCount=n:3000|SunAngularSize=n:21|MoonAngularSize=n:11;;",
        "Clouds:Instance;;Cover=n:0.5|Density=n:0.7|Color=c3:255,255,255|Enabled=b:true;;",
        "PostEffect:Instance;A;Enabled=b:true;;",
        "BloomEffect:PostEffect;;Intensity=n:1|Size=n:24|Threshold=n:2;;",
        "BlurEffect:PostEffect;;Size=n:24;;",
        "ColorCorrectionEffect:PostEffect;;Brightness=n:0|Contrast=n:0|Saturation=n:0|TintColor=c3:255,255,255;;",
        "SunRaysEffect:PostEffect;;Intensity=n:0.25|Spread=n:1;;",
        "DepthOfFieldEffect:PostEffect;;FarIntensity=n:0.75|FocusDistance=n:0.05|InFocusRadius=n:10|NearIntensity=n:0.75;;",
        "ColorGradingEffect:PostEffect;;;;",
        "ReplicatedStorage:Instance;AS;;;",
        "ReplicatedFirst:Instance;AS;;;RemoveDefaultLoadingScreen,IsFinishedReplicating",
        "ServerScriptService:Instance;AS;LoadStringEnabled=b:false;;",
        "ServerStorage:Instance;AS;;;",
        "StarterGui:Instance;AS;ShowDevelopmentGui=b:true|ResetPlayerGuiOnSpawn=b:true|"
            + "ScreenOrientation=E:Font.Legacy;;SetCoreGuiEnabled,GetCoreGuiEnabled,SetCore,GetCore",
        "StarterPack:Instance;AS;;;",
        "StarterPlayer:Instance;AS;CharacterWalkSpeed=n:16|CharacterJumpPower=n:50|CharacterJumpHeight=n:7.2|"
            + "CharacterUseJumpPower=b:true|CameraMaxZoomDistance=n:128|CameraMinZoomDistance=n:0.5|"
            + "AutoJumpEnabled=b:true|HealthDisplayDistance=n:100|NameDisplayDistance=n:100|"
            + "LoadCharacterAppearance=b:true|CharacterMaxSlopeAngle=n:89|EnableMouseLockOption=b:true|"
            + "DevComputerMovementMode=E:DevComputerMovementMode.UserChoice;;",
        "StarterPlayerScripts:Instance;A;;;",
        "StarterCharacterScripts:StarterPlayerScripts;A;;;",
        "SoundService:Instance;AS;AmbientReverb=E:Font.Legacy|DistanceFactor=n:3.33|RolloffScale=n:1|"
            + "RespectFilteringEnabled=b:true;;PlayLocalSound,SetListener,GetListener",
        "SoundGroup:Instance;;Volume=n:0.5;;",
        "Chat:Instance;AS;BubbleChatEnabled=b:false|LoadDefaultChat=b:true;Chatted;Chat,FilterStringAsync,"
            + "FilterStringForBroadcast,CanUserChatAsync",
        "TextChatService:Instance;AS;ChatVersion=E:Font.Legacy|CreateDefaultCommands=b:true|"
            + "CreateDefaultTextChannels=b:true;MessageReceived,SendingMessage;CanUserChatAsync,CanUsersChatAsync,"
            + "DisplayBubble",
        "TextChannel:Instance;;;MessageReceived;SendAsync,DisplaySystemMessage,AddUserAsync",
        "TextChatCommand:Instance;;PrimaryAlias=s:|SecondaryAlias=s:|Enabled=b:true;Triggered;",
        "TextService:Instance;AS;;;GetTextSize,FilterStringAsync,GetTextBoundsAsync",
        "RunService:Instance;AS;;Heartbeat,RenderStepped,Stepped,PreSimulation,PostSimulation,PreRender,"
            + "PreAnimation;IsClient,IsServer,IsStudio,IsRunning,IsRunMode,IsEdit,BindToRenderStep,UnbindFromRenderStep",
        "UserInputService:Instance;AS;TouchEnabled=b:false|KeyboardEnabled=b:true|MouseEnabled=b:true|"
            + "GamepadEnabled=b:false|AccelerometerEnabled=b:false|GyroscopeEnabled=b:false|VREnabled=b:false|"
            + "MouseBehavior=E:MouseBehavior.Default|MouseIconEnabled=b:true|MouseDeltaSensitivity=n:1|"
            + "ModalEnabled=b:false;InputBegan,InputChanged,InputEnded,TouchStarted,TouchMoved,TouchEnded,"
            + "TouchTap,TouchLongPress,TouchPinch,TouchSwipe,JumpRequest,LastInputTypeChanged,GamepadConnected,"
            + "GamepadDisconnected,WindowFocused,WindowFocusReleased,TextBoxFocused,TextBoxFocusReleased;"
            + "IsKeyDown,IsMouseButtonPressed,GetKeysPressed,GetMouseLocation,GetMouseDelta,GetLastInputType,"
            + "GetFocusedTextBox,GetConnectedGamepads,GetNavigationGamepads,IsGamepadButtonDown,GetGamepadState,"
            + "GetStringForKeyCode,GetMouseButtonsPressed,GetPlatform",
        "ContextActionService:Instance;AS;;LocalToolEquipped,LocalToolUnequipped;BindAction,BindActionAtPriority,"
            + "UnbindAction,UnbindAllActions,GetBoundActionInfo,GetAllBoundActionInfo,SetTitle,SetImage,"
            + "SetPosition,SetDescription,GetButton",
        "GuiService:Instance;AS;SelectedObject=R|AutoSelectGuiEnabled=b:true|MenuIsOpen=b:false;MenuOpened,"
            + "MenuClosed;GetGuiInset,IsTenFootInterface,OpenBrowserWindow",
        "TweenService:Instance;AS;;;Create,GetValue,SmoothDamp",
        "TweenBase:Instance;A;PlaybackState=E:PlaybackState.Begin;Completed;Play,Pause,Cancel",
        "Tween:TweenBase;A;Instance=R|TweenInfo=R;;",
        "Debris:Instance;AS;MaxItems=n:1000;;AddItem,addItem",
        "CollectionService:Instance;AS;;TagAdded,TagRemoved;AddTag,RemoveTag,HasTag,GetTagged,GetTags,"
            + "GetInstanceAddedSignal,GetInstanceRemovedSignal,GetAllTags",
        "HttpService:Instance;AS;HttpEnabled=b:false;;JSONEncode,JSONDecode,GenerateGUID,UrlEncode,GetAsync,"
            + "PostAsync,RequestAsync",
        "DataStoreService:Instance;AS;;;GetDataStore,GetGlobalDataStore,GetOrderedDataStore,GetRequestBudgetForRequestType,"
            + "ListDataStoresAsync",
        "GlobalDataStore:Instance;A;;;GetAsync,SetAsync,UpdateAsync,RemoveAsync,IncrementAsync,OnUpdate",
        "DataStore:GlobalDataStore;A;;;ListKeysAsync,GetVersionAsync,ListVersionsAsync",
        "OrderedDataStore:GlobalDataStore;A;;;GetSortedAsync",
        "DataStorePages:Instance;A;IsFinished=b:false;;GetCurrentPage,AdvanceToNextPageAsync",
        "FriendPages:DataStorePages;A;;;",
        "MemoryStoreService:Instance;AS;;;GetSortedMap,GetQueue,GetHashMap",
        "MarketplaceService:Instance;AS;ProcessReceipt=R;PromptPurchaseFinished,PromptGamePassPurchaseFinished,"
            + "PromptProductPurchaseFinished,PromptPremiumPurchaseFinished,PromptSubscriptionPurchaseFinished;"
            + "PromptPurchase,PromptGamePassPurchase,PromptProductPurchase,PromptPremiumPurchase,"
            + "PromptSubscriptionPurchase,UserOwnsGamePassAsync,PlayerOwnsAsset,GetProductInfo,"
            + "GetDeveloperProductsAsync,PlayerOwnsBundle,GetSubscriptionProductInfoAsync,GetUserSubscriptionStatusAsync",
        "BadgeService:Instance;AS;;;AwardBadge,UserHasBadgeAsync,GetBadgeInfoAsync,CheckUserBadgesAsync",
        "MessagingService:Instance;AS;;;PublishAsync,SubscribeAsync",
        "TeleportService:Instance;AS;;TeleportInitFailed,LocalPlayerArrivedFromTeleport;Teleport,TeleportAsync,"
            + "TeleportToPlaceInstance,TeleportToPrivateServer,ReserveServer,GetLocalPlayerTeleportData,"
            + "SetTeleportGui,GetPlayerPlaceInstanceAsync",
        "TeleportOptions:Instance;;ReservedServerAccessCode=s:|ServerInstanceId=s:|ShouldReserveServer=b:false;;"
            + "SetTeleportData,GetTeleportData",
        "PathfindingService:Instance;AS;;;CreatePath,FindPathAsync",
        "Path:Instance;A;Status=E:PathStatus.NoPath;Blocked,Unblocked;ComputeAsync,GetWaypoints,CheckOcclusionAsync",
        "PathfindingModifier:Instance;;Label=s:|PassThrough=b:false;;",
        "PhysicsService:Instance;AS;;;RegisterCollisionGroup,UnregisterCollisionGroup,CollisionGroupSetCollidable,"
            + "CollisionGroupsAreCollidable,GetRegisteredCollisionGroups,IsCollisionGroupRegistered,"
            + "RenameCollisionGroup,CreateCollisionGroup,SetPartCollisionGroup",
        "SocialService:Instance;AS;;GameInvitePromptClosed;PromptGameInvite,CanSendGameInviteAsync,"
            + "PromptPhoneBook,CanSendCallInviteAsync",
        "AnalyticsService:Instance;AS;;;LogEconomyEvent,LogFunnelStepEvent,LogOnboardingFunnelStepEvent,"
            + "LogProgressionEvent,LogProgressionStartEvent,LogProgressionCompleteEvent,LogProgressionFailEvent,"
            + "LogCustomEvent,FireEvent,FireCustomEvent",
        "PolicyService:Instance;AS;;;GetPolicyInfoForPlayerAsync",
        "LocalizationService:Instance;AS;RobloxLocaleId=s:en-us|SystemLocaleId=s:en-us;;GetTranslatorForPlayer,"
            + "GetTranslatorForPlayerAsync,GetCountryRegionForPlayerAsync",
        "GroupService:Instance;AS;;;GetGroupInfoAsync,GetGroupsAsync",
        "InsertService:Instance;AS;;;LoadAsset,LoadAssetVersion,GetFreeModels",
        "AssetService:Instance;AS;;;CreateEditableImage,CreateEditableMesh,GetGamePlacesAsync",
        "ProximityPromptService:Instance;AS;Enabled=b:true|MaxPromptsVisible=n:16;PromptTriggered,PromptShown,"
            + "PromptHidden,PromptButtonHoldBegan,PromptButtonHoldEnded;",
        "Camera:Instance;;CFrame=cf|Focus=cf|FieldOfView=n:70|CameraType=E:CameraType.Custom|CameraSubject=R|"
            + "ViewportSize=v2:800,600|NearPlaneZ=n:-0.1|HeadScale=n:1|DiagonalFieldOfView=n:88;;"
            + "WorldToScreenPoint,WorldToViewportPoint,ScreenPointToRay,ViewportPointToRay,ZoomToExtents,"
            + "GetPartsObscuringTarget,Interpolate",
        "Sound:Instance;;SoundId=s:|Volume=n:0.5|Looped=b:false|Playing=b:false|PlaybackSpeed=n:1|"
            + "TimePosition=n:0|TimeLength=n:0|RollOffMaxDistance=n:10000|RollOffMinDistance=n:10|"
            + "RollOffMode=E:RollOffMode.Inverse|PlayOnRemove=b:false|IsPlaying=b:false|IsLoaded=b:true|"
            + "SoundGroup=R;Ended,Played,Paused,Resumed,Stopped,Loaded,DidLoop;Play,Stop,Pause,Resume",
        "SoundEffect:Instance;A;Enabled=b:true|Priority=n:0;;",
        "ReverbSoundEffect:SoundEffect;;;;",
        "EqualizerSoundEffect:SoundEffect;;;;",
        "ParticleEmitter:Instance;;Rate=n:20|Lifetime=nr:5,10|Speed=nr:5,5|SpreadAngle=v2:0,0|"
            + "Acceleration=v3:0,0,0|Color=cs:255,255,255|Size=ns:1|Transparency=ns:0|Texture=s:|"
            + "Enabled=b:true|LightEmission=n:0|LightInfluence=n:1|Rotation=nr:0,0|RotSpeed=nr:0,0|Drag=n:0|"
            + "EmissionDirection=E:NormalId.Top|LockedToPart=b:false|ZOffset=n:0|Orientation=E:ParticleOrientation.FacingCamera|"
            + "Brightness=n:1|Squash=ns:0|TimeScale=n:1;;Emit,Clear",
        "Fire:Instance;;Color=c3:236,139,70|SecondaryColor=c3:139,80,55|Heat=n:9|Size=n:5|Enabled=b:true|"
            + "TimeScale=n:1;;",
        "Smoke:Instance;;Color=c3:255,255,255|Opacity=n:0.5|RiseVelocity=n:1|Size=n:1|Enabled=b:true|TimeScale=n:1;;",
        "Sparkles:Instance;;SparkleColor=c3:144,25,255|Enabled=b:true|TimeScale=n:1;;",
        "Explosion:Instance;;Position=v3:0,0,0|BlastRadius=n:4|BlastPressure=n:500000|DestroyJointRadiusPercent=n:1|"
            + "ExplosionType=E:Font.Legacy|Visible=b:true|TimeScale=n:1;Hit;",
        "Light:Instance;A;Brightness=n:1|Color=c3:255,255,255|Enabled=b:true|Shadows=b:false;;",
        "PointLight:Light;;Range=n:8;;",
        "SpotLight:Light;;Range=n:16|Angle=n:90|Face=E:NormalId.Front;;",
        "SurfaceLight:Light;;Range=n:16|Angle=n:90|Face=E:NormalId.Front;;",
        "Highlight:Instance;;Adornee=R|Enabled=b:true|FillColor=c3:255,0,0|FillTransparency=n:0.5|"
            + "OutlineColor=c3:255,255,255|OutlineTransparency=n:0|DepthMode=E:Font.Legacy;;",
        "Beam:Instance;;Attachment0=R|Attachment1=R|Color=cs:255,255,255|Transparency=ns:0.5|Width0=n:1|Width1=n:1|"
            + "Texture=s:|TextureSpeed=n:1|TextureLength=n:1|LightEmission=n:0|FaceCamera=b:false|Enabled=b:true|"
            + "Segments=n:10|CurveSize0=n:0|CurveSize1=n:0;;SetTextureOffset",
        "Trail:Instance;;Attachment0=R|Attachment1=R|Color=cs:255,255,255|Transparency=ns:0.5|Lifetime=n:2|"
            + "MinLength=n:0.1|Enabled=b:true|FaceCamera=b:false|LightEmission=n:0|Texture=s:|WidthScale=ns:1;;Clear",
        "SelectionBox:Instance;;Adornee=R|Color3=c3:13,105,172|LineThickness=n:0.15|SurfaceTransparency=n:1|Visible=b:true;;",
        "ClickDetector:Instance;;MaxActivationDistance=n:32|CursorIcon=s:;MouseClick,RightMouseClick,"
            + "MouseHoverEnter,MouseHoverLeave;",
        "ProximityPrompt:Instance;;ActionText=s:Interact|ObjectText=s:|HoldDuration=n:0|"
            + "KeyboardKeyCode=E:KeyCode.E|GamepadKeyCode=E:KeyCode.ButtonX|MaxActivationDistance=n:10|"
            + "RequiresLineOfSight=b:true|Enabled=b:true|ClickablePrompt=b:true|Exclusivity=E:Font.Legacy|"
            + "UIOffset=v2:0,0|Style=E:Font.Legacy;Triggered,TriggerEnded,PromptShown,PromptHidden,"
            + "PromptButtonHoldBegan,PromptButtonHoldEnded;InputHoldBegin,InputHoldEnd",
        "BackpackItem:Instance;A;TextureId=s:;;",
        "Tool:BackpackItem;;RequiresHandle=b:true|CanBeDropped=b:true|ToolTip=s:|Grip=cf|Enabled=b:true|"
            + "ManualActivationOnly=b:false;Activated,Deactivated,Equipped,Unequipped;Activate,Deactivate",
        "HopperBin:BackpackItem;;;;",
        "GuiBase:Instance;A;;;",
        "GuiBase2d:GuiBase;A;AbsolutePosition=v2:0,0|AbsoluteSize=v2:0,0|AbsoluteRotation=n:0|AutoLocalize=b:true;;",
        "LayerCollector:GuiBase2d;A;Enabled=b:true|ResetOnSpawn=b:true|ZIndexBehavior=E:ZIndexBehavior.Sibling;;",
        "ScreenGui:LayerCollector;;IgnoreGuiInset=b:false|DisplayOrder=n:0|ScreenInsets=E:Font.Legacy|"
            + "ClipToDeviceSafeArea=b:true;;",
        "BillboardGui:LayerCollector;;Adornee=R|Size=u2:0,0,0,0|StudsOffset=v3:0,0,0|ExtentsOffset=v3:0,0,0|"
            + "AlwaysOnTop=b:false|MaxDistance=n:100000|LightInfluence=n:1|SizeOffset=v2:0,0|Brightness=n:1;;",
        "SurfaceGui:LayerCollector;;Adornee=R|Face=E:NormalId.Front|CanvasSize=v2:800,600|AlwaysOnTop=b:false|"
            + "LightInfluence=n:1|PixelsPerStud=n:50|SizingMode=E:Font.Legacy|Brightness=n:1;;",
        "GuiObject:GuiBase2d;A;Position=u2:0,0,0,0|Size=u2:0,100,0,100|AnchorPoint=v2:0,0|"
            + "BackgroundColor3=c3:255,255,255|BackgroundTransparency=n:0|BorderColor3=c3:27,42,53|"
            + "BorderSizePixel=n:1|Visible=b:true|ZIndex=n:1|LayoutOrder=n:0|Rotation=n:0|ClipsDescendants=b:false|"
            + "Active=b:false|Selectable=b:false|AutomaticSize=E:AutomaticSize.None|SizeConstraint=E:SizeConstraint.RelativeXY|"
            + "Interactable=b:true|Transparency=n:0;InputBegan,InputChanged,InputEnded,MouseEnter,MouseLeave,"
            + "MouseMoved,MouseWheelForward,MouseWheelBackward,TouchTap,TouchLongPress,SelectionGained,SelectionLost;"
            + "TweenPosition,TweenSize,TweenSizeAndPosition",
        "Frame:GuiObject;;;;",
        "CanvasGroup:GuiObject;;GroupTransparency=n:0|GroupColor3=c3:255,255,255;;",
        "ScrollingFrame:GuiObject;;CanvasSize=u2:0,0,2,0|CanvasPosition=v2:0,0|ScrollBarThickness=n:12|"
            + "ScrollingDirection=E:FillDirection.Vertical|ScrollingEnabled=b:true|"
            + "ScrollBarImageColor3=c3:0,0,0|AutomaticCanvasSize=E:AutomaticSize.None|ElasticBehavior=E:Font.Legacy|"
            + "AbsoluteCanvasSize=v2:0,0|AbsoluteWindowSize=v2:0,0;;",
        "TextLabel:GuiObject;;Text=s:Label|TextColor3=c3:0,0,0|TextSize=n:14|Font=E:Font.SourceSans|"
            + "TextScaled=b:false|TextWrapped=b:false|TextXAlignment=E:TextXAlignment.Center|"
            + "TextYAlignment=E:TextYAlignment.Center|TextTransparency=n:0|TextStrokeColor3=c3:0,0,0|"
            + "TextStrokeTransparency=n:1|RichText=b:false|LineHeight=n:1|MaxVisibleGraphemes=n:-1|"
            + "TextBounds=v2:0,0|TextFits=b:true|ContentText=s:;;",
        "TextButton:TextLabel;;Text=s:Button|AutoButtonColor=b:true|Modal=b:false|Selected=b:false;"
            + "MouseButton1Click,MouseButton1Down,MouseButton1Up,MouseButton2Click,MouseButton2Down,"
            + "MouseButton2Up,Activated;",
        "TextBox:TextLabel;;Text=s:TextBox|PlaceholderText=s:|PlaceholderColor3=c3:178,178,178|"
            + "ClearTextOnFocus=b:true|MultiLine=b:false|TextEditable=b:true|CursorPosition=n:1;FocusLost,"
            + "Focused,ReturnPressedFromOnScreenKeyboard;CaptureFocus,ReleaseFocus,IsFocused",
        "ImageLabel:GuiObject;;Image=s:|ImageColor3=c3:255,255,255|ImageTransparency=n:0|ScaleType=E:ScaleType.Stretch|"
            + "SliceCenter=R|SliceScale=n:1|TileSize=u2:1,0,1,0|ImageRectOffset=v2:0,0|ImageRectSize=v2:0,0|"
            + "ResampleMode=E:Font.Legacy|IsLoaded=b:true;;",
        "ImageButton:ImageLabel;;AutoButtonColor=b:true|HoverImage=s:|PressedImage=s:|Modal=b:false|Selected=b:false;"
            + "MouseButton1Click,MouseButton1Down,MouseButton1Up,MouseButton2Click,MouseButton2Down,"
            + "MouseButton2Up,Activated;",
        "ViewportFrame:GuiObject;;CurrentCamera=R|Ambient=c3:200,200,200|LightColor=c3:140,140,140|"
            + "LightDirection=v3:-1,-1,-1|ImageColor3=c3:255,255,255|ImageTransparency=n:0;;",
        "VideoFrame:GuiObject;;Video=s:|Playing=b:false|Looped=b:false|Volume=n:1;Ended,Loaded,Paused,Played;Play,Pause",
        "UIComponent:Instance;A;;;",
        "UIConstraint:UIComponent;A;;;",
        "UILayout:UIComponent;A;;;",
        "UIGridStyleLayout:UILayout;A;SortOrder=E:SortOrder.LayoutOrder|FillDirection=E:FillDirection.Vertical|"
            + "HorizontalAlignment=E:HorizontalAlignment.Left|VerticalAlignment=E:VerticalAlignment.Top|"
            + "AbsoluteContentSize=v2:0,0;;ApplyLayout",
        "UIListLayout:UIGridStyleLayout;;Padding=u:0,0|Wraps=b:false|HorizontalFlex=E:Font.Legacy|"
            + "VerticalFlex=E:Font.Legacy|ItemLineAlignment=E:Font.Legacy;;",
        "UIGridLayout:UIGridStyleLayout;;CellPadding=u2:0,5,0,5|CellSize=u2:0,100,0,100|FillDirection=E:FillDirection.Horizontal|"
            + "FillDirectionMaxCells=n:0|StartCorner=E:Font.Legacy;;",
        "UITableLayout:UIGridStyleLayout;;;;",
        "UIPageLayout:UIGridStyleLayout;;Animated=b:true|Circular=b:false|CurrentPage=R|Padding=u:0,0|"
            + "EasingStyle=E:EasingStyle.Back|TweenTime=n:1;PageEnter,PageLeave,Stopped;JumpTo,JumpToIndex,Next,Previous",
        "UIPadding:UIComponent;;PaddingTop=u:0,0|PaddingBottom=u:0,0|PaddingLeft=u:0,0|PaddingRight=u:0,0;;",
        "UICorner:UIComponent;;CornerRadius=u:0,8;;",
        "UIStroke:UIComponent;;Color=c3:0,0,0|Thickness=n:1|Transparency=n:0|Enabled=b:true|"
            + "ApplyStrokeMode=E:Font.Legacy|LineJoinMode=E:Font.Legacy;;",
        "UIGradient:UIComponent;;Color=cs:255,255,255|Transparency=ns:0|Offset=v2:0,0|Rotation=n:0|Enabled=b:true;;",
        "UIScale:UIComponent;;Scale=n:1;;",
        "UIAspectRatioConstraint:UIConstraint;;AspectRatio=n:1|AspectType=E:Font.Legacy|DominantAxis=E:Font.Legacy;;",
        "UISizeConstraint:UIConstraint;;MinSize=v2:0,0|MaxSize=v2:1e9,1e9;;",
        "UITextSizeConstraint:UIConstraint;;MinTextSize=n:1|MaxTextSize=n:100;;",
        "UIFlexItem:UIComponent;;FlexMode=E:Font.Legacy|GrowRatio=n:0|ShrinkRatio=n:0;;",
        "Dialog:Instance;;InitialPrompt=s:|Purpose=E:Font.Legacy|Tone=E:Font.Legacy|ConversationDistance=n:25|"
            + "InUse=b:false;DialogChoiceSelected;",
        "DialogChoice:Instance;;UserDialog=s:|ResponseDialog=s:|GoodbyeDialog=s:;;",
        "Hint:Instance;;Text=s:;;",
        "Message:Instance;;Text=s:;;",
        "ForceField:Instance;;Visible=b:true;;",
        "Hole:Instance;;;;",
        "Glue:JointInstance;;;;",
        "ObjectValueRef:Instance;A;;;",
        "WrapLayer:Instance;;;;",
        "WrapTarget:Instance;;;;",
        "SurfaceAppearance:Instance;;ColorMap=s:|NormalMap=s:|RoughnessMap=s:|MetalnessMap=s:|Color=c3:255,255,255|"
            + "AlphaMode=E:Font.Legacy;;",
        "MaterialVariant:Instance;;BaseMaterial=E:Material.Plastic|ColorMap=s:|NormalMap=s:|RoughnessMap=s:|"
            + "MetalnessMap=s:|StudsPerTile=n:10;;",
        "MaterialService:Instance;AS;Use2022Materials=b:true;;GetMaterialVariant,GetBaseMaterialOverride,"
            + "SetBaseMaterialOverride",
        "Actor:Model;;;;BindToMessage,BindToMessageParallel,SendMessage",
        "BrickyardService:Instance;AS;;LeaderboardChanged;SetLeaderstat,GetLeaderboardAsync,AwardXP,"
            + "CompleteQuest,GetPlayerLevel,PromptInvite,LogFunnel",
    };

    /// A class description.
    public static final class Desc {
        public final String name;
        public final Desc parent;
        final boolean creatable;
        final boolean service;
        final LinkedHashMap<String, Object> defaults = new LinkedHashMap<String, Object>();
        final Set<String> events = new HashSet<String>();
        final Set<String> methods = new HashSet<String>();
        Behavior behavior;

        Desc(String name, Desc parent, boolean creatable, boolean service) {
            this.name = name;
            this.parent = parent;
            this.creatable = creatable;
            this.service = service;
        }

        public boolean isA(String n) {
            for (Desc d = this; d != null; d = d.parent) {
                if (d.name.equals(n)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isCreatable() {
            return creatable;
        }

        public boolean isService() {
            return service;
        }

        /// The default of `prop`, [#MISSING] when no class in the chain has it.
        public Object defaultOf(String prop) {
            for (Desc d = this; d != null; d = d.parent) {
                if (d.defaults.containsKey(prop)) {
                    return d.defaults.get(prop);
                }
            }
            return MISSING;
        }

        public boolean hasEvent(String n) {
            for (Desc d = this; d != null; d = d.parent) {
                if (d.events.contains(n)) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasMethod(String n) {
            for (Desc d = this; d != null; d = d.parent) {
                if (d.methods.contains(n)) {
                    return true;
                }
            }
            return false;
        }

        /// Every property of the chain, base class first.
        public List<String> allProperties() {
            List<Desc> chain = new ArrayList<Desc>();
            for (Desc d = this; d != null; d = d.parent) {
                chain.add(0, d);
            }
            List<String> out = new ArrayList<String>();
            for (Desc d : chain) {
                for (String k : d.defaults.keySet()) {
                    if (!out.contains(k)) {
                        out.add(k);
                    }
                }
            }
            return out;
        }

        public List<String> allMethods() {
            List<String> out = new ArrayList<String>();
            for (Desc d = this; d != null; d = d.parent) {
                out.addAll(d.methods);
            }
            return out;
        }

        public List<String> allEvents() {
            List<String> out = new ArrayList<String>();
            for (Desc d = this; d != null; d = d.parent) {
                out.addAll(d.events);
            }
            return out;
        }

        public Behavior getBehavior() {
            return behavior;
        }
    }

    /// Returned by [Desc#defaultOf] for an unknown property.
    public static final Object MISSING = new Object();

    private static final Map<String, Desc> CLASSES = new LinkedHashMap<String, Desc>();

    static {
        for (String line : DEFS) {
            parse(line);
        }
        Behaviors.install();
    }

    private ClassDb() {
    }

    public static Desc get(String name) {
        return CLASSES.get(name);
    }

    public static List<String> names() {
        return new ArrayList<String>(CLASSES.keySet());
    }

    static void attach(String cls, Behavior b) {
        Desc d = CLASSES.get(cls);
        if (d == null) {
            throw new IllegalArgumentException(cls);
        }
        d.behavior = b;
    }

    private static String[] split(String s, char sep) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == sep) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[0]);
    }

    private static void parse(String line) {
        String[] f = split(line, ';');
        int c = f[0].indexOf(':');
        String name = f[0].substring(0, c);
        String sup = f[0].substring(c + 1);
        Desc parent = sup.length() == 0 ? null : CLASSES.get(sup);
        if (sup.length() > 0 && parent == null) {
            throw new IllegalStateException("class " + name + " extends unknown " + sup);
        }
        String flags = f[1];
        Desc d = new Desc(name, parent, flags.indexOf('A') < 0, flags.indexOf('S') >= 0);
        if (f[2].length() > 0) {
            for (String p : split(f[2], '|')) {
                int eq = p.indexOf('=');
                d.defaults.put(p.substring(0, eq), value(p.substring(eq + 1)));
            }
        }
        if (f.length > 3 && f[3].length() > 0) {
            for (String e : split(f[3], ',')) {
                d.events.add(e);
            }
        }
        if (f.length > 4 && f[4].length() > 0) {
            for (String m : split(f[4], ',')) {
                d.methods.add(m);
            }
        }
        CLASSES.put(name, d);
    }

    private static double[] nums(String s) {
        String[] p = split(s, ',');
        double[] r = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            r[i] = Double.parseDouble(p[i]);
        }
        return r;
    }

    static Object value(String v) {
        if ("R".equals(v)) {
            return REF;
        }
        if ("cf".equals(v)) {
            return CFrame.IDENTITY;
        }
        int c = v.indexOf(':');
        String t = v.substring(0, c);
        String s = v.substring(c + 1);
        switch (t) {
            case "b":
                return Boolean.valueOf("true".equals(s));
            case "n":
                return Args.n(Double.parseDouble(s));
            case "s":
                return s;
            case "v3": {
                double[] n = nums(s);
                return new Vector3(n[0], n[1], n[2]);
            }
            case "v2": {
                double[] n = nums(s);
                return new Vector2(n[0], n[1]);
            }
            case "c3": {
                double[] n = nums(s);
                return new Color3(n[0] / 255, n[1] / 255, n[2] / 255);
            }
            case "u2": {
                double[] n = nums(s);
                return new UDim2(n[0], n[1], n[2], n[3]);
            }
            case "u": {
                double[] n = nums(s);
                return new UDim(n[0], n[1]);
            }
            case "E": {
                int dot = s.indexOf('.');
                return Enums.item(s.substring(0, dot), s.substring(dot + 1));
            }
            case "bc":
                return BrickColor.byNumber(Integer.parseInt(s));
            case "nr": {
                double[] n = nums(s);
                return new NumberRange(n[0], n[1]);
            }
            case "ns":
                return Sequence.number(Double.parseDouble(s));
            case "cs": {
                double[] n = nums(s);
                return Sequence.color(new Color3(n[0] / 255, n[1] / 255, n[2] / 255));
            }
            default:
                throw new IllegalStateException("bad default " + v);
        }
    }
}
