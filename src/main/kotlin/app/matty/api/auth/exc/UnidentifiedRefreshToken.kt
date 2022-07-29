package app.matty.api.auth.exc

class UnidentifiedRefreshToken : SecurityLevelException("Refresh token not found in storage!")