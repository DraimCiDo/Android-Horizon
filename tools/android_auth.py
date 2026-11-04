"""Public Microsoft application configuration embedded at APK build time."""
import uuid


def configuration(client_id):
    value = (client_id or '').strip()
    if not value:
        return {'microsoft_client_id': ''}
    try:
        parsed = uuid.UUID(value)
    except ValueError as error:
        raise ValueError('HORIZON_MSA_CLIENT_ID must be an application client ID (UUID)') from error
    if parsed.int == 0:
        raise ValueError('The all-zero placeholder is not a Microsoft application client ID')
    return {'microsoft_client_id': str(parsed)}
