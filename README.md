# Secure Key Backup for Android

This library can be used to backup secrets such as private keys or master passwords. Backups are encrypted and can be stored anywhere, e.g. in a cloud.

The key needed to decrypt a backup can be split into multiple parts which will be created by Shamir's secret sharing algorithm. This way only a subset of key parts is needed to restore an encrypted backup.


