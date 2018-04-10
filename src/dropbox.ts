import { Dropbox } from 'dropbox';

function dbxClient(token?: string): Dropbox {
  const ret = new Dropbox({clientId: process.env.DROPBOX_KEY, accessToken: token});
  (ret as any).setClientSecret(process.env.DROPBOX_SECRET);
  return ret;
}

export default dbxClient;
