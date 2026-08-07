import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.eshop.app',
  appName: 'E-Shop',
  webDir: 'www',
  bundledWebRuntime: false,
  server: {
    allowNavigation: [
      "*"
    ],
    cleartext: true
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 3000,
      launchAutoHide: true,
      backgroundColor: "#ffffff",
      androidSplashResourceName: "splash",
      androidScaleType: "CENTER_CROP",
      showSpinner: true,
      spinnerColor: "#dc2626"
    },
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#dc2626'
    },
    PushNotifications: {
      presentationOptions: ["badge", "sound", "alert"]
    }
  }
};

export default config;
