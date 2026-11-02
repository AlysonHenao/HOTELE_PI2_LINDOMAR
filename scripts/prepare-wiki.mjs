// Prepara la copia local de la Wiki; no hace commit ni push.
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const wiki = path.resolve(process.argv[2] || path.join(root, '..', 'HOTELE_PI2_LINDOMAR.wiki'));
const config = await fs.readFile(path.join(wiki, '.git', 'config'), 'utf8');
if (!config.includes('AlysonHenao/HOTELE_PI2_LINDOMAR.wiki.git')) {
  throw new Error('El destino debe ser la copia local de la Wiki de Hotel Lindomar');
}
const pages = {
  'README.md': 'Sprint-3',
  'USABILIDAD.md': 'Sprint-3-Usabilidad',
  'MANUAL-ADMINISTRADOR.md': 'Sprint-3-Manual-Administrador',
  'PRESUPUESTO-OPERACION.md': 'Sprint-3-Presupuesto-Operacion',
  'DESPLIEGUE-AWS.md': 'Sprint-3-Despliegue-AWS',
  'CALIDAD.md': 'Sprint-3-Calidad',
  'REVISION-PRS.md': 'Sprint-3-Revision-PRs',
};
const raw = 'https://raw.githubusercontent.com/wiki/AlysonHenao/HOTELE_PI2_LINDOMAR/';
for (const [filename, title] of Object.entries(pages)) {
  let content = await fs.readFile(path.join(root, 'docs', 'sprint3', filename), 'utf8');
  for (const [source, target] of Object.entries(pages)) {
    content = content.replaceAll(`](${source})`, `](${target})`);
  }
  content = content.replaceAll('](images/', `](${raw}sprint3-images/`)
    .replaceAll('](evidence/', `](${raw}sprint3-evidence/`);
  await fs.writeFile(path.join(wiki, `${title}.md`), content);
}
for (const [source, target] of [['images', 'sprint3-images'], ['evidence', 'sprint3-evidence']]) {
  await fs.mkdir(path.join(wiki, target), { recursive: true });
  const files = await fs.readdir(path.join(root, 'docs', 'sprint3', source));
  for (const file of files.filter((file) => !file.endsWith('.log'))) {
    await fs.copyFile(path.join(root, 'docs', 'sprint3', source, file), path.join(wiki, target, file));
  }
}
const homePath = path.join(wiki, 'Home.md');
const home = await fs.readFile(homePath, 'utf8');
if (!home.includes('](Sprint-3)')) {
  await fs.writeFile(homePath, `${home.trimEnd()}\n* [Sprint 3 — trabajo y evidencia de Julián](Sprint-3)\n`);
}
console.log(`Preparadas ${Object.keys(pages).length} secciones en ${wiki}. Publicación pendiente.`);
