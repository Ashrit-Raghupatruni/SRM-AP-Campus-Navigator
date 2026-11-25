// SRM University-AP exact campus center coordinates (derived from verified buildings GIS)
export const CAMPUS_LAT = 16.46408;
export const CAMPUS_LNG = 80.50688;

// Strict Campus Boundaries: locks panning/zooming to SRM AP only
export const CAMPUS_BOUNDS: [[number, number], [number, number]] = [
  [16.4580, 80.5020], // Southwest corner
  [16.4700, 80.5120], // Northeast corner
];

const RAD = Math.PI / 180;
const DEG = 180 / Math.PI;

/**
 * Calculates solar position and sunrise/sunset times for a given date and location.
 * Uses standard astronomical solar equations (Jean Meeus / NOAA algorithm).
 */
export function getSolarTimes(date: Date, lat: number = CAMPUS_LAT, lng: number = CAMPUS_LNG) {
  const startOfDay = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const dayOfYear = Math.floor((date.getTime() - new Date(date.getFullYear(), 0, 0).getTime()) / (1000 * 60 * 60 * 24));
  
  // Approximate solar declination
  const declination = 23.45 * Math.sin(RAD * ((360 / 365) * (dayOfYear - 81)));
  const decRad = declination * RAD;
  const latRad = lat * RAD;

  // Hour angle for sunrise/sunset (sun zenith = 90.833 deg for standard sunrise/sunset)
  const zenith = 90.833 * RAD;
  const cosH = (Math.cos(zenith) - Math.sin(latRad) * Math.sin(decRad)) / (Math.cos(latRad) * Math.cos(decRad));

  // Clamped for polar limits
  const clampedCosH = Math.max(-1, Math.min(1, cosH));
  const hourAngleDeg = Math.acos(clampedCosH) * DEG;

  // Solar noon in hours UTC
  const equationOfTime = (y: number) => {
    // Spencer formula approx in minutes
    const B = (360 / 365) * (dayOfYear - 81) * RAD;
    return 9.87 * Math.sin(2 * B) - 7.53 * Math.cos(B) - 1.5 * Math.sin(B);
  };
  const eqTimeMin = equationOfTime(dayOfYear);
  
  // Local solar noon in local time
  const timezoneOffsetMinutes = -date.getTimezoneOffset();
  const solarNoonMinutes = 720 - (4 * lng) - eqTimeMin + timezoneOffsetMinutes;
  
  const sunriseMinutes = solarNoonMinutes - (hourAngleDeg * 4);
  const sunsetMinutes = solarNoonMinutes + (hourAngleDeg * 4);

  // Civil Twilight (sun at 96 deg)
  const civilZenith = 96 * RAD;
  const cosHCivil = (Math.cos(civilZenith) - Math.sin(latRad) * Math.sin(decRad)) / (Math.cos(latRad) * Math.cos(decRad));
  const civilH = Math.acos(Math.max(-1, Math.min(1, cosHCivil))) * DEG;
  const dawnMinutes = solarNoonMinutes - (civilH * 4);
  const duskMinutes = solarNoonMinutes + (civilH * 4);

  const makeDate = (minutesFromMidnight: number) => {
    const d = new Date(startOfDay);
    d.setMinutes(minutesFromMidnight);
    return d;
  };

  const sunrise = makeDate(sunriseMinutes);
  const sunset = makeDate(sunsetMinutes);
  const dawn = makeDate(dawnMinutes);
  const dusk = makeDate(duskMinutes);
  const goldenHourMorning = makeDate(sunriseMinutes + 45);
  const goldenHourEvening = makeDate(sunsetMinutes - 45);

  // Solar altitude and azimuth at the current exact time
  const currentMinutes = date.getHours() * 60 + date.getMinutes() + date.getSeconds() / 60;
  const currentHourAngle = ((currentMinutes - solarNoonMinutes) / 4) * RAD;

  const sinAlt = Math.sin(latRad) * Math.sin(decRad) + Math.cos(latRad) * Math.cos(decRad) * Math.cos(currentHourAngle);
  const altitude = Math.asin(Math.max(-1, Math.min(1, sinAlt)));

  const cosAz = (Math.sin(decRad) - Math.sin(latRad) * Math.sin(altitude)) / (Math.cos(latRad) * Math.cos(altitude));
  let azimuth = Math.acos(Math.max(-1, Math.min(1, cosAz)));
  if (Math.sin(currentHourAngle) > 0) {
    azimuth = 2 * Math.PI - azimuth;
  }

  const isNight = date < dawn || date > dusk;
  const isGoldenHour = (date >= sunrise && date <= goldenHourMorning) || (date >= goldenHourEvening && date <= sunset);

  const timeOfDayProgress = currentMinutes / 1440; // 0.0 at 00:00, 0.5 at 12:00, 1.0 at 24:00

  return {
    sunrise,
    sunset,
    dawn,
    dusk,
    goldenHourMorning,
    goldenHourEvening,
    isNight,
    isGoldenHour,
    sunAltitude: altitude,
    sunAzimuth: azimuth,
    timeOfDayProgress,
  };
}

export function formatTime(date: Date): string {
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });
}
