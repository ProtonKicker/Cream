# EventBus-generated implementations are loaded via Class.forName
# in EventBus.java:84, so they must not be renamed or removed.
-keep class ru.ytkab0bp.eventbus.impl.** { *; }
-keep class * implements ru.ytkab0bp.eventbus.EventBusListenerImpl { *; }

# Klippy/Moonraker per-slot services are instantiated via Class.forName
# in KlipperInstance.kt (KlippyService_<slot> / MoonrakerService_<slot>).
-keep class ru.ytkab0bp.beamklipper.service.KlippyService_* { *; }
-keep class ru.ytkab0bp.beamklipper.service.MoonrakerService_* { *; }

# Chaquopy runtime classes and the Python bridge.
-keep class com.chaquo.python.** { *; }
-keep class ru.ytkab0bp.beamklipper.KlipperApp { *; }
