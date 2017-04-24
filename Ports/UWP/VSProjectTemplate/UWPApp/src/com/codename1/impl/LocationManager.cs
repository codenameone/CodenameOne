using System;
using Windows.Devices.Geolocation;
using Windows.Foundation;

namespace com.codename1.impl
{
    public class LocationManager : location.LocationManager
    {

        private Geolocator watcher;
        private Geoposition lastPosition;
        private bool requestedAccess;

        public LocationManager()
        {
     
        }

        protected override void bindListener()
        {
            SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, async () =>
            {
                var r = await Geolocator.RequestAccessAsync();

            }).AsTask().GetAwaiter().GetResult();
            if (watcher == null)
            {
                watcher = new Geolocator()
                {
                    MovementThreshold = 1,
                    DesiredAccuracy = PositionAccuracy.High,
                    ReportInterval = (uint)TimeSpan.FromSeconds(1).TotalMilliseconds,  ///FA 1s is used in track/navi apps
                };
            }
            this.watcher.StatusChanged += new TypedEventHandler<Geolocator, StatusChangedEventArgs>(watcher_StatusChanged);
            this.watcher.PositionChanged += new TypedEventHandler<Geolocator, PositionChangedEventArgs>(watcher_PositionChanged);
        }

        async void watcher_StatusChanged(Geolocator sender, StatusChangedEventArgs eventArgs)
        {
            
            switch (eventArgs.Status)
            {
                case PositionStatus.Disabled:
                    setStatus(OUT_OF_SERVICE);
                    com.codename1.ui.Display.getInstance().callSerially(new LocationManagerStatusUpdater(getLocationListener(), OUT_OF_SERVICE));
                    break;
                case PositionStatus.Initializing:
                case PositionStatus.NoData:
                    setStatus(TEMPORARILY_UNAVAILABLE);
                    com.codename1.ui.Display.getInstance().callSerially(new LocationManagerStatusUpdater(getLocationListener(), TEMPORARILY_UNAVAILABLE));
                    break;
                case PositionStatus.Ready:
                    setStatus(AVAILABLE);
                    com.codename1.ui.Display.getInstance().callSerially(new LocationManagerStatusUpdater(getLocationListener(), AVAILABLE));
                    break;
            }
        }

        async void watcher_PositionChanged(Geolocator sender, PositionChangedEventArgs e)
        {
            lastPosition = await sender.GetGeopositionAsync();
            if (lastPosition != null && lastPosition.Coordinate.AltitudeAccuracy.HasValue)
            {
                com.codename1.ui.Display.getInstance().callSerially(new LocationManagerLocationUpdater(getLocationListener(), convert(lastPosition)));
                //getLocationListener().locationUpdated(convert(lastPosition));
            }
        }

      protected override void clearListener()
        {
            if (watcher != null)
            {
                watcher.StatusChanged -= new TypedEventHandler<Geolocator, StatusChangedEventArgs>(watcher_StatusChanged);
                watcher.PositionChanged -= new TypedEventHandler<Geolocator, PositionChangedEventArgs>(watcher_PositionChanged);
            }
        }

        public override location.Location getCurrentLocation()
        {
            if (lastPosition != null)
            {
                return convert(lastPosition);
            }
            return null;
        }

        public override location.Location getLastKnownLocation()
        {
            if (lastPosition != null)
            {
                return convert(lastPosition);
            }
            return null;
        }

        private location.Location convert(Geoposition position)
        {
            location.Location location = new location.Location();
            location.setTimeStamp(position.Coordinate.Timestamp.UtcTicks / 10000);
            location.setLatitude(position.Coordinate.Point.Position.Latitude);
            location.setLongitude(position.Coordinate.Point.Position.Longitude);
            location.setAltitude((float)position.Coordinate.Point.Position.Altitude);
            location.setDirection((float)position.Coordinate.Heading);
            location.setVelocity((float)position.Coordinate.Speed);
            location.setAccuracy((float)position.Coordinate.Accuracy); ///FA vertical is more usufull AltitudeAccuracy);
            location.setStatus(getStatus());
            return location;
        }

        

    }

    public class LocationManagerStatusUpdater : java.lang.Object, java.lang.Runnable
    {
        private int status;
        private location.LocationListener listener;

        public LocationManagerStatusUpdater(location.LocationListener listener, int status)
        {
            this.listener = listener;
            this.status = status;
        }

        public virtual void run()
        {
            if (listener != null)
            {
                listener.providerStateChanged(status);
            }
        }
    }

    public class LocationManagerLocationUpdater : java.lang.Object, java.lang.Runnable
    {
        private location.LocationListener listener;
        private location.Location location;

        public LocationManagerLocationUpdater(location.LocationListener listener, location.Location location)
        {
            this.listener = listener;
            this.location = location;
        }

        public virtual void run()
        {
            if (listener != null)
            {
                listener.locationUpdated(location);
            }

        }
    }
}
