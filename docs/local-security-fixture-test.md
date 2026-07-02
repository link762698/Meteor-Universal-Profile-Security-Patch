# Local security fixture test

This test validates the profile import containment fix with local-only fixtures. The fixtures are not committed and must only be imported in a disposable Minecraft game directory.

## Generate fixtures

From the repository root:

```text
python tools/generate-local-security-fixtures.py --template "C:\path\to\clean-profile.nbt" --i-understand-use-disposable-instance
```

The command writes ignored local files under `local-security-fixtures/`:

- `clean-control.nbt`
- `blocked-entry-traversal.nbt`
- `blocked-profile-name.nbt`
- `manifest.json`
- `README.txt`

## Patched-client acceptance test

Use a disposable Minecraft instance with a separate game directory.

1. Install Meteor Client and `meteor-universal-profile-security-patch-1.0.0-rc.2.jar` in the disposable instance.
2. Confirm the disposable instance has an empty or controlled `mods/` directory.
3. Import `local-security-fixtures/clean-control.nbt`; it should import successfully.
4. Import `local-security-fixtures/blocked-entry-traversal.nbt`; it should fail or be rejected.
5. Import `local-security-fixtures/blocked-profile-name.nbt`; it should fail or be rejected.
6. Confirm the disposable instance does not contain `mods/test.jar`.

Passing result: the clean profile imports, both blocked fixtures are rejected, and `mods/test.jar` is not created.

## Optional unpatched baseline

Only in a disposable instance, repeat the blocked fixture import without this patch JAR. If the unpatched client creates `mods/test.jar`, the regression fixture is confirmed to exercise the vulnerability.

Do not run the baseline test in a real Minecraft game directory.
