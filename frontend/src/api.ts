let csrf = '';
export async function initCsrf() {
  const response=await fetch('/api/csrf',{credentials:'same-origin'});
  if(!response.ok) throw new Error('No se pudo conectar con el servidor.');
  csrf=(await response.json()).token;
}
export async function api<T>(path:string, options:RequestInit = {}):Promise<T> {
  if(options.method && options.method!=='GET' && !csrf) await initCsrf();
  const response=await fetch('/api'+path,{...options,credentials:'same-origin',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':csrf,...options.headers}});
  if(response.status===204) return undefined as T;
  const data=await response.json().catch(()=>({message:'No se pudo conectar con el servidor.'}));
  if(!response.ok) throw new Error(data.message || 'No se pudo completar la operación.');
  return data;
}
export const post = <T>(path:string,body:unknown) => api<T>(path,{method:'POST',body:JSON.stringify(body)});
