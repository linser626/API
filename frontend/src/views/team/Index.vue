<template>
  <div class="team-page">
    <template v-if="!selectedTeam">
      <div class="page-header flex-between">
        <h2>{{ t('team.createTeam') }}</h2>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          {{ t('team.createTeam') }}
        </el-button>
      </div>

      <div class="team-list" v-loading="loading">
        <el-empty v-if="!loading && teams.length === 0" :description="t('common.noData')" />
        <div class="team-cards">
          <el-card
            v-for="team in teams"
            :key="team.id"
            shadow="hover"
            class="team-card"
            @click="selectTeam(team)"
          >
            <div class="team-card-header">
              <el-avatar :size="48" :icon="UserFilled" />
              <div class="team-card-info">
                <div class="team-card-name">{{ team.name }}</div>
                <div class="team-card-desc">{{ team.description || t('common.noDesc') }}</div>
              </div>
            </div>
            <div class="team-card-stats">
              <div class="stat-item">
                <el-icon><User /></el-icon>
                <span>{{ t('team.memberCount') }}: {{ team.memberCount }}/{{ team.maxMembers }}</span>
              </div>
              <div class="stat-item">
                <el-icon><Wallet /></el-icon>
                <span>{{ t('team.teamBalance') }}: {{ formatMoney(team.balance) }}</span>
              </div>
              <div class="stat-item">
                <el-tag :type="roleTagType(team.myRole)" size="small">
                  {{ roleLabel(team.myRole) }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="page-header">
        <el-button @click="selectedTeam = null" :icon="ArrowLeft" circle />
        <h2 style="margin-left: 12px;">{{ selectedTeam.name }}</h2>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('team.overview')" name="overview">
          <el-card shadow="hover" class="mb-20">
            <div class="team-detail-section">
              <div class="detail-row">
                <span class="detail-label">{{ t('team.teamName') }}</span>
                <span class="detail-value">{{ teamDetail.name }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">{{ t('team.owner') }}</span>
                <span class="detail-value">{{ teamDetail.ownerName }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">{{ t('team.memberCount') }}</span>
                <span class="detail-value">{{ teamDetail.memberCount }} / {{ teamDetail.maxMembers }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">{{ t('team.myRole') }}</span>
                <el-tag :type="roleTagType(teamDetail.myRole)" size="small">{{ roleLabel(teamDetail.myRole) }}</el-tag>
              </div>
              <div class="detail-row">
                <span class="detail-label">{{ t('team.teamBalance') }}</span>
                <span class="detail-value balance-value">{{ formatMoney(teamDetail.balance) }}</span>
              </div>
              <div class="detail-row" v-if="teamDetail.description">
                <span class="detail-label">{{ t('common.description') }}</span>
                <span class="detail-value">{{ teamDetail.description }}</span>
              </div>
            </div>
            <div class="team-actions">
              <el-button
                v-if="isAdminRole(teamDetail.myRole)"
                type="primary"
                @click="showRechargeDialog"
              >
                <el-icon><Wallet /></el-icon>
                {{ t('billing.recharge') }}
              </el-button>
              <el-button
                v-if="isAdminRole(teamDetail.myRole)"
                @click="showEditTeamDialog"
              >
                <el-icon><Edit /></el-icon>
                {{ t('common.edit') }}
              </el-button>
              <el-button
                v-if="teamDetail.myRole === 'owner'"
                type="danger"
                @click="handleDeleteTeam"
              >
                <el-icon><Delete /></el-icon>
                {{ t('common.delete') }}
              </el-button>
              <el-button
                v-if="teamDetail.myRole && teamDetail.myRole !== 'owner'"
                type="warning"
                @click="handleLeaveTeam"
              >
                {{ t('team.leaveTeam') }}
              </el-button>
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane :label="t('team.members')" name="members">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header flex-between">
                <span class="card-title">{{ t('team.members') }}</span>
                <el-button
                  v-if="isAdminRole(teamDetail.myRole)"
                  type="primary"
                  size="small"
                  @click="showInviteDialog"
                >
                  <el-icon><Plus /></el-icon>
                  {{ t('team.inviteMember') }}
                </el-button>
              </div>
            </template>

            <el-table :data="members" v-loading="membersLoading" stripe>
              <el-table-column prop="username" :label="t('auth.username')" min-width="120" />
              <el-table-column prop="nickname" :label="t('team.nickname')" min-width="100" />
              <el-table-column :label="t('team.myRole')" width="120" align="center">
                <template #default="{ row }">
                  <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('apikey.createdAt')" width="180" align="center">
                <template #default="{ row }">
                  {{ formatDate(row.joinedAt) }}
                </template>
              </el-table-column>
              <el-table-column :label="t('apikey.actions')" width="200" align="center" v-if="isAdminRole(teamDetail?.myRole)">
                <template #default="{ row }">
                  <el-select
                    v-if="row.role !== 'owner' && teamDetail?.myRole === 'owner'"
                    :model-value="row.role"
                    size="small"
                    style="width: 100px;"
                    @change="(val) => handleRoleChange(row, val)"
                  >
                    <el-option label="管理员" value="admin" />
                    <el-option label="成员" value="member" />
                  </el-select>
                  <el-button
                    v-if="row.role !== 'owner' && isAdminRole(teamDetail?.myRole)"
                    type="danger"
                    link
                    size="small"
                    @click="handleRemoveMember(row)"
                  >
                    {{ t('team.removeMember') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <el-tab-pane :label="t('team.apiKeys')" name="apikeys">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header flex-between">
                <span class="card-title">{{ t('team.apiKeys') }}</span>
                <el-button
                  v-if="isAdminRole(teamDetail.myRole)"
                  type="primary"
                  size="small"
                  @click="showCreateKeyDialog"
                >
                  <el-icon><Plus /></el-icon>
                  {{ t('apikey.createKey') }}
                </el-button>
              </div>
            </template>

            <el-table :data="apiKeys" v-loading="keysLoading" stripe>
              <el-table-column prop="name" :label="t('apikey.keyName')" min-width="120" />
              <el-table-column :label="t('apikey.keyValue')" min-width="200">
                <template #default="{ row }">
                  <code class="key-text">{{ row.keyValue }}</code>
                </template>
              </el-table-column>
              <el-table-column :label="t('apikey.status')" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? t('apikey.active') : t('apikey.disabled') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('apikey.createdAt')" width="180" align="center">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column :label="t('apikey.actions')" width="120" align="center" v-if="isAdminRole(teamDetail?.myRole)">
                <template #default="{ row }">
                  <el-button type="danger" link size="small" @click="handleRevokeKey(row)">
                    {{ t('apikey.revokeKey') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="createDialogVisible" :title="t('team.createTeam')" width="500px" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="100px">
        <el-form-item :label="t('team.teamName')" prop="name">
          <el-input v-model="createForm.name" :placeholder="t('team.teamNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.description')" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="3" :placeholder="t('team.descPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('team.maxMembers')" prop="maxMembers">
          <el-input-number v-model="createForm.maxMembers" :min="2" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreateTeam">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editTeamDialogVisible" :title="t('common.edit')" width="500px">
      <el-form :model="editTeamForm" label-width="100px">
        <el-form-item :label="t('team.teamName')">
          <el-input v-model="editTeamForm.name" />
        </el-form-item>
        <el-form-item :label="t('common.description')">
          <el-input v-model="editTeamForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('team.maxMembers')">
          <el-input-number v-model="editTeamForm.maxMembers" :min="2" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editTeamDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleUpdateTeam">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inviteDialogVisible" :title="t('team.inviteMember')" width="500px" @close="resetInviteForm">
      <el-form ref="inviteFormRef" :model="inviteForm" :rules="inviteFormRules" label-width="100px">
        <el-form-item :label="t('auth.email')" prop="email">
          <el-input v-model="inviteForm.email" :placeholder="t('team.inviteEmailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('team.myRole')">
          <el-select v-model="inviteForm.role">
            <el-option label="管理员" value="admin" />
            <el-option label="成员" value="member" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleInviteMember">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createKeyDialogVisible" :title="t('apikey.createKey')" width="500px">
      <el-form :model="createKeyForm" label-width="100px">
        <el-form-item :label="t('apikey.keyName')">
          <el-input v-model="createKeyForm.name" :placeholder="t('apikey.keyName')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createKeyDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreateKey">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="keyDisplayVisible" :title="t('team.keyCreated')" width="500px">
      <el-alert
        :title="t('team.keyCreatedWarning')"
        type="warning"
        :closable="false"
        show-icon
        class="mb-20"
      />
      <div class="key-display">
        <code>{{ createdKeyValue }}</code>
        <el-button type="primary" size="small" @click="copyKey(createdKeyValue)">
          <el-icon><CopyDocument /></el-icon>
          {{ t('apikey.copyKey') }}
        </el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="rechargeDialogVisible" :title="t('billing.recharge')" width="480px">
      <el-form :model="rechargeForm" label-width="100px">
        <el-form-item :label="t('billing.rechargeAmount')">
          <el-input-number v-model="rechargeForm.amount" :min="1" :max="10000" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item :label="t('team.quickAmount')">
          <div class="quick-amounts">
            <el-button
              v-for="amount in quickAmounts"
              :key="amount"
              :type="rechargeForm.amount === amount ? 'primary' : 'default'"
              @click="rechargeForm.amount = amount"
            >
              ¥{{ amount }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleRecharge">{{ t('common.confirm') }} ¥{{ rechargeForm.amount }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getMyTeams, getTeamDetail, createTeam, updateTeam, deleteTeam as deleteTeamApi, getTeamMembers, inviteMember, removeMember, updateMemberRole, leaveTeam as leaveTeamApi, createTeamApiKey, getTeamApiKeys, revokeTeamApiKey, rechargeTeamBalance } from '@/api/team'
import { formatMoney, formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import Clipboard from 'clipboard'

const { t } = useI18n()

const loading = ref(false)
const submitLoading = ref(false)
const teams = ref([])
const selectedTeam = ref(null)
const teamDetail = ref({})
const activeTab = ref('overview')

const members = ref([])
const membersLoading = ref(false)

const apiKeys = ref([])
const keysLoading = ref(false)

const createDialogVisible = ref(false)
const editTeamDialogVisible = ref(false)
const inviteDialogVisible = ref(false)
const createKeyDialogVisible = ref(false)
const keyDisplayVisible = ref(false)
const rechargeDialogVisible = ref(false)

const createdKeyValue = ref('')

const createFormRef = ref(null)
const inviteFormRef = ref(null)

const createForm = reactive({
  name: '',
  description: '',
  maxMembers: 10
})

const createFormRules = {
  name: [
    { required: true, message: t('team.teamNameRequired'), trigger: 'blur' },
    { min: 2, max: 100, message: t('team.teamNameLength'), trigger: 'blur' }
  ]
}

const editTeamForm = reactive({
  name: '',
  description: '',
  maxMembers: 10
})

const inviteForm = reactive({
  email: '',
  role: 'member'
})

const inviteFormRules = {
  email: [
    { required: true, message: t('team.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('team.emailInvalid'), trigger: 'blur' }
  ]
}

const createKeyForm = reactive({
  name: 'default'
})

const rechargeForm = reactive({
  amount: 100
})

const quickAmounts = [10, 50, 100, 200, 500]

const roleTagType = (role) => {
  const map = { owner: 'danger', admin: 'warning', member: 'info' }
  return map[role] || 'info'
}

const roleLabel = (role) => {
  const map = { owner: t('team.owner'), admin: t('team.admin'), member: t('team.member') }
  return map[role] || role
}

const isAdminRole = (role) => role === 'owner' || role === 'admin'

const loadTeams = async () => {
  loading.value = true
  try {
    const res = await getMyTeams()
    teams.value = res.data || []
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const selectTeam = async (team) => {
  selectedTeam.value = team
  activeTab.value = 'overview'
  await loadTeamDetail(team.id)
}

const loadTeamDetail = async (teamId) => {
  try {
    const res = await getTeamDetail(teamId)
    teamDetail.value = res.data || {}
  } catch (error) {
    // handled by interceptor
  }
}

const loadMembers = async () => {
  if (!selectedTeam.value) return
  membersLoading.value = true
  try {
    const res = await getTeamMembers(selectedTeam.value.id)
    members.value = res.data || []
  } catch (error) {
    // handled by interceptor
  } finally {
    membersLoading.value = false
  }
}

const loadApiKeys = async () => {
  if (!selectedTeam.value) return
  keysLoading.value = true
  try {
    const res = await getTeamApiKeys(selectedTeam.value.id)
    apiKeys.value = res.data || []
  } catch (error) {
    // handled by interceptor
  } finally {
    keysLoading.value = false
  }
}

const showCreateDialog = () => {
  createForm.name = ''
  createForm.description = ''
  createForm.maxMembers = 10
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  createFormRef.value?.resetFields()
}

const handleCreateTeam = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await createTeam(createForm)
    ElMessage.success(t('common.success'))
    createDialogVisible.value = false
    loadTeams()
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const showEditTeamDialog = () => {
  editTeamForm.name = teamDetail.value.name || ''
  editTeamForm.description = teamDetail.value.description || ''
  editTeamForm.maxMembers = teamDetail.value.maxMembers || 10
  editTeamDialogVisible.value = true
}

const handleUpdateTeam = async () => {
  submitLoading.value = true
  try {
    await updateTeam(selectedTeam.value.id, editTeamForm)
    ElMessage.success(t('common.success'))
    editTeamDialogVisible.value = false
    loadTeamDetail(selectedTeam.value.id)
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleDeleteTeam = () => {
  ElMessageBox.confirm(t('team.deleteConfirm'), t('common.confirm'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTeamApi(selectedTeam.value.id)
      ElMessage.success(t('common.success'))
      selectedTeam.value = null
      loadTeams()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const showInviteDialog = () => {
  inviteForm.email = ''
  inviteForm.role = 'member'
  inviteDialogVisible.value = true
}

const resetInviteForm = () => {
  inviteFormRef.value?.resetFields()
}

const handleInviteMember = async () => {
  const valid = await inviteFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await inviteMember(selectedTeam.value.id, inviteForm)
    ElMessage.success(t('common.success'))
    inviteDialogVisible.value = false
    loadMembers()
    loadTeamDetail(selectedTeam.value.id)
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleRemoveMember = (row) => {
  ElMessageBox.confirm(t('team.removeConfirm'), t('common.confirm'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    try {
      await removeMember(selectedTeam.value.id, row.id)
      ElMessage.success(t('common.success'))
      loadMembers()
      loadTeamDetail(selectedTeam.value.id)
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const handleRoleChange = async (row, newRole) => {
  try {
    await updateMemberRole(selectedTeam.value.id, row.id, newRole)
    ElMessage.success(t('common.success'))
    loadMembers()
  } catch (error) {
    // handled by interceptor
  }
}

const handleLeaveTeam = () => {
  ElMessageBox.confirm(t('team.leaveConfirm'), t('common.confirm'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    try {
      await leaveTeamApi(selectedTeam.value.id)
      ElMessage.success(t('common.success'))
      selectedTeam.value = null
      loadTeams()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const showCreateKeyDialog = () => {
  createKeyForm.name = 'default'
  createKeyDialogVisible.value = true
}

const handleCreateKey = async () => {
  submitLoading.value = true
  try {
    const res = await createTeamApiKey(selectedTeam.value.id, createKeyForm)
    if (res.data?.keyValue) {
      createdKeyValue.value = res.data.keyValue
      keyDisplayVisible.value = true
    }
    ElMessage.success(t('common.success'))
    createKeyDialogVisible.value = false
    loadApiKeys()
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleRevokeKey = (row) => {
  ElMessageBox.confirm(t('apikey.revokeConfirm'), t('common.confirm'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    try {
      await revokeTeamApiKey(selectedTeam.value.id, row.id)
      ElMessage.success(t('common.success'))
      loadApiKeys()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const copyKey = (key) => {
  const clipboard = new Clipboard('.team-page', {
    text: () => key
  })
  clipboard.on('success', () => {
    ElMessage.success(t('common.copySuccess'))
    clipboard.destroy()
  })
  clipboard.on('error', () => {
    ElMessage.error(t('common.copyFailed'))
    clipboard.destroy()
  })
  clipboard.onClick({ currentTarget: document.querySelector('.team-page') })
}

const showRechargeDialog = () => {
  rechargeForm.amount = 100
  rechargeDialogVisible.value = true
}

const handleRecharge = async () => {
  if (rechargeForm.amount <= 0) {
    ElMessage.warning(t('team.amountRequired'))
    return
  }
  submitLoading.value = true
  try {
    await rechargeTeamBalance(selectedTeam.value.id, rechargeForm.amount)
    ElMessage.success(t('common.success'))
    rechargeDialogVisible.value = false
    loadTeamDetail(selectedTeam.value.id)
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

import { watch } from 'vue'
watch(activeTab, (val) => {
  if (val === 'members') loadMembers()
  if (val === 'apikeys') loadApiKeys()
})

onMounted(() => {
  loadTeams()
})
</script>

<style lang="scss" scoped>
.team-page {
  .team-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
  }

  .team-card {
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .team-card-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      .team-card-info {
        flex: 1;
        min-width: 0;

        .team-card-name {
          font-size: 16px;
          font-weight: 600;
          color: var(--color-text-primary);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .team-card-desc {
          font-size: 13px;
          color: var(--color-text-secondary);
          margin-top: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .team-card-stats {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: var(--color-text-regular);
      }
    }
  }

  .team-detail-section {
    .detail-row {
      display: flex;
      align-items: center;
      padding: 10px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .detail-label {
        width: 100px;
        font-size: 14px;
        color: var(--color-text-secondary);
        flex-shrink: 0;
      }

      .detail-value {
        font-size: 14px;
        color: var(--color-text-primary);

        &.balance-value {
          font-size: 20px;
          font-weight: 700;
          color: var(--color-primary);
        }
      }
    }
  }

  .team-actions {
    display: flex;
    gap: 12px;
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid #f0f0f0;
  }

  .key-text {
    font-family: 'Courier New', monospace;
    font-size: 13px;
    background-color: #f5f7fa;
    padding: 4px 8px;
    border-radius: 4px;
    color: var(--color-text-regular);
  }

  .key-display {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background-color: #f5f7fa;
    border-radius: 8px;

    code {
      flex: 1;
      font-family: 'Courier New', monospace;
      font-size: 14px;
      word-break: break-all;
      color: var(--color-text-primary);
    }
  }

  .quick-amounts {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .card-header {
    .card-title {
      font-size: 16px;
      font-weight: 600;
    }
  }
}
</style>
