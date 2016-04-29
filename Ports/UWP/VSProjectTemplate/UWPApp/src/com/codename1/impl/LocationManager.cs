using System;
using Windows.Devices.Geolocation;
using Windows.Foundation;

namespace com.codename1.impl
{
    public class LocationManager : location.LocationManager
    {

        private Geolocator watcher;
        private Geoposition lastPosition;

        public LocationManager()
        {
     
        }

        protected override void bindListener()
        {

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

        void watcher_StatusChanged(Geolocator sender, StatusChangedEventArgs eventArgs)
        {
            switch (eventArgs.Status)
            {
                case PositionStatus.Disabled:
                    setStatus(OUT_OF_SERVICE);
                    break;
                case PositionStatus.Initializing:
                case PositionStatus.NoData:
                    setStatus(TEMPORARILY_UNAVAILABLE);
                    break;
                case PositionStatus.Ready:
                    setStatus(AVAILABLE);
                    break;
            }
        }

        async void watcher_PositionChanged(Geolocator sender, PositionChangedEventArgs e)
        {
            lastPosition = await sender.GetGeopositionAsync();
            if (lastPosition != null && lastPosition.Coordinate.AltitudeAccuracy.HasValue)
            {
                getLocationListener().locationUpdated(convert(lastPosition));
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
}
