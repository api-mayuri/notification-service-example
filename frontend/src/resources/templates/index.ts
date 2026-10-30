import DescriptionIcon from '@mui/icons-material/Description';
import { TemplateList } from './TemplateList';
import { TemplateShow } from './TemplateShow';
import { TemplateCreate } from './TemplateCreate';

export const templates = {
    list: TemplateList,
    show: TemplateShow,
    create: TemplateCreate,
    icon: DescriptionIcon,
    recordRepresentation: (record: { key: string; channel: string; locale: string }) =>
        `${record.key} · ${record.channel}/${record.locale}`,
};
